package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fc.v2.mapper.auto.TPostRectifyCardMapper;
import com.fc.v2.mapper.auto.TPostRectifyStageMapper;
import com.fc.v2.model.auto.TPostRectifyCard;
import com.fc.v2.model.auto.TPostRectifyStage;

/**
 * 责令整改事务卡状态机单测：四段相邻过口、履历推算段次、压层不同屏、封口冻结、在办唯一。
 *
 * @author fuce
 * @date 2026-09-30
 */
@ExtendWith(MockitoExtension.class)
public class TPostRectifyCardServiceImplTest {

    @Mock
    private TPostRectifyCardMapper cardMapper;

    @Mock
    private TPostRectifyStageMapper stageMapper;

    @InjectMocks
    private TPostRectifyCardServiceImpl service;

    private final List<TPostRectifyStage> ledger = new ArrayList<TPostRectifyStage>();
    private final AtomicLong stageIds = new AtomicLong(1);
    private int openCards = 0;

    @BeforeEach
    public void setUp() {
        ledger.clear();
        stageIds.set(1);
        openCards = 0;
        lenient().when(stageMapper.selectList(any())).thenAnswer(inv -> new ArrayList<TPostRectifyStage>(ledger));
        lenient().when(stageMapper.insert(any(TPostRectifyStage.class))).thenAnswer(inv -> {
            TPostRectifyStage row = inv.getArgument(0);
            row.setId(stageIds.getAndIncrement());
            ledger.add(row);
            return 1;
        });
        lenient().when(cardMapper.selectCount(any())).thenAnswer(inv -> openCards);
        lenient().when(cardMapper.updateById(any(TPostRectifyCard.class))).thenReturn(1);
    }

    private TPostRectifyCard stubIssue(Date deadline) {
        TPostRectifyCard card = new TPostRectifyCard();
        card.setOutletNo("WD-001");
        card.setPunishBillNo("HC-2025-009");
        card.setOutletAddr("青屏县东街 12 号");
        card.setRectifyDeadline(deadline);
        when(cardMapper.insert(any(TPostRectifyCard.class))).thenAnswer(inv -> {
            card.setId(1001L);
            return 1;
        });
        lenient().when(cardMapper.selectById(1001L)).thenReturn(card);
        TPostRectifyCard issued = service.issue(card);
        // 每用例只起一张卡，起完后把"在办计数"摆成 1，模拟库中真实状态。
        openCards = 1;
        return issued;
    }

    private Date daysFromNow(int days) {
        return new Date(System.currentTimeMillis() + days * 24L * 3600 * 1000);
    }

    private TPostRectifyStage lastRow() {
        return ledger.get(ledger.size() - 1);
    }

    private long countStage(int stageNo) {
        return ledger.stream().filter(r -> stageNo == r.getStageNo()).count();
    }

    @Test
    public void issueWritesIssueStageAndActivates() {
        TPostRectifyCard issued = stubIssue(daysFromNow(7));
        assertNotNull(issued);
        assertEquals(0, issued.getStage().intValue());
        assertEquals(1, issued.getStatus().intValue());
        assertNotNull(issued.getBizNo());
        assertEquals(1, ledger.size());
        assertEquals("ISSUE", ledger.get(0).getAction());
        assertTrue(ledger.get(0).getEntryNote().contains("HC-2025-009"));
        assertTrue(ledger.get(0).getEntryNote().contains("青屏县东街 12 号"));
    }

    @Test
    public void issueRejectsMissingRequiredFields() {
        TPostRectifyCard card = new TPostRectifyCard();
        card.setOutletNo("WD-001");
        assertNull(service.issue(card));
    }

    @Test
    public void secondOpenCardForSameOutletRejected() {
        stubIssue(daysFromNow(7));
        TPostRectifyCard second = new TPostRectifyCard();
        second.setOutletNo("WD-001");
        second.setPunishBillNo("HC-2");
        second.setOutletAddr("别处");
        second.setRectifyDeadline(daysFromNow(3));
        assertNull(service.issue(second));
    }

    @Test
    public void fourStagesWalkOnlyAdjacentAndCloseSeals() {
        TPostRectifyCard card = stubIssue(daysFromNow(7));

        Date finishAt = daysFromNow(2);
        TPostRectifyCard rectifying = service.advance(1001L, "企业上报：已补备案三处", finishAt);
        assertEquals(1, rectifying.getStage().intValue());
        assertEquals(1, rectifying.getStatus().intValue());
        assertEquals(finishAt.getTime() / 1000, lastRow().getFinishAt().getTime() / 1000);

        TPostRectifyCard reviewing = service.advance(1001L, "现场点验合格", (Date) null);
        assertEquals(2, reviewing.getStage().intValue());
        assertEquals(0, lastRow().getOverdue().intValue());

        TPostRectifyCard closed = service.advance(1001L, "合卡", "HZ-2026-001");
        assertEquals(3, closed.getStage().intValue());
        assertEquals(2, closed.getStatus().intValue());
        assertEquals("HZ-2026-001", closed.getCloseReceiptNo());

        // 销案后不能再接段、不能收回。
        assertNull(service.advance(1001L, "还想动", (Date) null));
        assertNull(service.rollback(1001L, "想退回"));
        assertEquals(4, ledger.size());
    }

    @Test
    public void closeWithoutReceiptRejected() {
        stubIssue(daysFromNow(7));
        service.advance(1001L, "整改", daysFromNow(1));
        service.advance(1001L, "复查");
        assertNull(service.advance(1001L, "销案但没回执", (String) null));
        assertEquals(3, ledger.size());
    }

    @Test
    public void screenStageIsIgnoredReckonedFromLedger() {
        TPostRectifyCard card = stubIssue(daysFromNow(7));
        // 屏上的号被写成销案（3），履历实际只走到下达（0）。
        card.setStage(3);
        TPostRectifyCard moved = service.advance(1001L, "企业整改上报", daysFromNow(1));
        // 以算出来的号为准：只许落到整改中（1），跳不到复查。
        assertEquals(1, moved.getStage().intValue());
        assertEquals(1, lastRow().getStageNo().intValue());
    }

    @Test
    public void sameStageSecondVisitAddsNoNote() {
        stubIssue(daysFromNow(7));
        service.advance(1001L, "整改上报", daysFromNow(1));
        // 人停在整改中，再往前只能进复查，没有任何把手能往整改中多落一句。
        TPostRectifyCard next = service.advance(1001L, "复查一趟", (Date) null);
        assertEquals(2, next.getStage().intValue());
        assertEquals(1, countStage(1));
        int rowsBefore = ledger.size();
        // 复查现场跑第二趟、报第二回：往前的唯一落点是销案，销案没回执进不去——
        // 第二回不许在复查段多落一句记事，账上只认先前那一遍。
        assertNull(service.advance(1001L, "复查第二趟（不得落账）", (Date) null));
        assertEquals(rowsBefore, ledger.size());
        assertEquals(1, countStage(2));
    }

    @Test
    public void rollbackPushesOldLayerDownAndNewLayerStandsAlone() {
        stubIssue(daysFromNow(7));
        service.advance(1001L, "整改上报", daysFromNow(1));
        service.advance(1001L, "复查不合格");
        // 复查没过：收回整改中重来。
        TPostRectifyCard back = service.rollback(1001L, "重报：再改两处", daysFromNow(3));
        assertEquals(1, back.getStage().intValue());
        assertEquals(2, countStage(1)); // 旧层压底 + 后补层，账上两层都在
        // 当前视图每段只摆最大一层：整改中只显示后补的 BACK 层，旧层不与它同屏并列。
        TPostRectifyStage shown = back.getStages().stream().filter(s -> s.getStageNo() == 1).findFirst().get();
        assertEquals("BACK", shown.getAction());
        assertEquals(1, shown.getLayerNo().intValue());
        assertEquals("重报：再改两处", shown.getEntryNote());
        // 再往前只走相邻一格，落复查第二层。
        TPostRectifyCard review2 = service.advance(1001L, "复查合格");
        assertEquals(2, review2.getStage().intValue());
        assertEquals(2, countStage(2));
    }

    @Test
    public void haltSealsCardAtCurrentStage() {
        stubIssue(daysFromNow(7));
        service.advance(1001L, "整改上报", daysFromNow(1));
        // 就在当前段（整改中）点明不再往下走：段次不挪，卡就地封口。
        TPostRectifyCard halted = service.advance(1001L, "企业停业，不再往下走", true);
        assertEquals(1, halted.getStage().intValue());
        assertEquals(3, halted.getStatus().intValue());
        assertEquals("HALT", lastRow().getAction());
        assertEquals(1, lastRow().getStageNo().intValue());
        assertNull(service.advance(1001L, "想继续", (Date) null));
        assertNull(service.rollback(1001L, "想退回"));
    }

    @Test
    public void overdueFlaggedAtReviewWhenDeadlinePassedWithoutReport() {
        stubIssue(daysFromNow(-1)); // 期限已过
        service.advance(1001L, "整改中（未报完成日）", (Date) null);
        TPostRectifyCard reviewing = service.advance(1001L, "复查");
        assertEquals(1, lastRow().getOverdue().intValue());
        assertEquals(2, reviewing.getStage().intValue());
    }

    @Test
    public void finishAfterDeadlineAlsoOverdue() {
        stubIssue(daysFromNow(1));
        service.advance(1001L, "整改", daysFromNow(5)); // 报来的完成日晚于期限
        service.advance(1001L, "复查");
        assertEquals(1, lastRow().getOverdue().intValue());
    }

    @Test
    public void rollbackFromIssueRejected() {
        stubIssue(daysFromNow(7));
        assertNull(service.rollback(1001L, "下达段无处可退"));
        assertEquals(1, ledger.size());
    }

    @Test
    public void rollbackFinishAtOnlyLandsOnRectifyStage() {
        stubIssue(daysFromNow(7));
        service.advance(1001L, "整改", daysFromNow(1));
        // 从整改中往回收，目标是下达，带完成日的重载与落点对不上，拒。
        assertNull(service.rollback(1001L, "退下达", daysFromNow(1)));
    }

    @Test
    public void emptyNoteRejected() {
        stubIssue(daysFromNow(7));
        assertNull(service.advance(1001L, " "));
        assertNull(service.rollback(1001L, ""));
    }

    @Test
    public void unknownCardRejected() {
        assertNull(service.advance(9999L, "x"));
        assertNull(service.rollback(9999L, "x"));
    }

    @Test
    public void ledgerReplaysFromCloseBackToIssue() {
        stubIssue(daysFromNow(7));
        service.advance(1001L, "整改", daysFromNow(1));
        service.advance(1001L, "复查");
        service.advance(1001L, "合卡", "HZ-1");
        // 从销案往回数：每段当前层各一笔，段次相邻无缺、无跳。
        List<TPostRectifyStage> view = service.selectTPostRectifyCardById(1001L).getStages();
        assertEquals(4, view.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(i, view.get(i).getStageNo().intValue());
        }
        // 走到头的卡履历只追加：没有任何更新/删除路径。
        assertFalse(ledger.isEmpty());
    }
}
