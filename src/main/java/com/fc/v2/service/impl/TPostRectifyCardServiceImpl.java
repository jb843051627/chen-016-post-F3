package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TPostRectifyCardMapper;
import com.fc.v2.mapper.auto.TPostRectifyStageMapper;
import com.fc.v2.model.auto.TPostRectifyCard;
import com.fc.v2.model.auto.TPostRectifyStage;
import com.fc.v2.service.ITPostRectifyCardService;
import com.fc.v2.util.StringUtils;

/**
 * 责令整改事务卡 Service业务层处理（state-machine 形状：单据流转）
 *
 * 一卡四段（下达0/整改中1/复查2/销案3），只能相邻过口：
 * 往前挪一格、往回退一格各自只落在这里；不设当场造卡（起卡另走 issue）、
 * 提笔涂改、整张抹去的把手。卡当下停在第几格，照段履历（t_post_rectify_stage）
 * 里已过完的笔数推算，屏上写的 stage 与承办人心里记的都当不得真。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TPostRectifyCardServiceImpl implements ITPostRectifyCardService {

    /** 段次：下达 */
    private static final int STAGE_ISSUE = 0;
    /** 段次：整改中 */
    private static final int STAGE_RECTIFY = 1;
    /** 段次：复查 */
    private static final int STAGE_REVIEW = 2;
    /** 段次：销案 */
    private static final int STAGE_CLOSE = 3;
    private static final int MAX_STAGE = STAGE_CLOSE;

    /** 卡的落点：在办 */
    private static final int STATUS_ACTIVE = 1;
    /** 卡的落点：已销案（合卡冻结） */
    private static final int STATUS_CLOSED = 2;
    /** 卡的落点：已打住（封口，不得再接段） */
    private static final int STATUS_HALT = 3;

    /** 过口名目 */
    private static final String ACTION_ISSUE = "ISSUE";
    private static final String ACTION_ENTER = "ENTER";
    private static final String ACTION_BACK = "BACK";
    private static final String ACTION_HALT = "HALT";

    @javax.annotation.Resource
    private TPostRectifyCardMapper postRectifyCardMapper;

    @javax.annotation.Resource
    private TPostRectifyStageMapper postRectifyStageMapper;

    @Override
    public TPostRectifyCard selectTPostRectifyCardById(Long id) {
        TPostRectifyCard card = this.postRectifyCardMapper.selectById(id);
        if (card == null) {
            return null;
        }
        attachAndReckon(card);
        return card;
    }

    @Override
    public List<TPostRectifyCard> selectTPostRectifyCardList(QueryWrapper<TPostRectifyCard> queryWrapper) {
        List<TPostRectifyCard> cards = this.postRectifyCardMapper.selectList(queryWrapper);
        for (TPostRectifyCard card : cards) {
            attachAndReckon(card);
        }
        return cards;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostRectifyCard issue(TPostRectifyCard card) {
        if (card == null
                || StringUtils.isEmpty(card.getOutletNo())
                || StringUtils.isEmpty(card.getPunishBillNo())
                || StringUtils.isEmpty(card.getOutletAddr())
                || card.getRectifyDeadline() == null) {
            return null;
        }
        // 一家网点名下同时只容一张在办的卡：头一张没销案也没打住之前，第二张立不起来。
        Integer openCnt = this.postRectifyCardMapper.selectCount(new QueryWrapper<TPostRectifyCard>()
                .eq("outlet_no", card.getOutletNo())
                .eq("status", STATUS_ACTIVE)
                .eq("del_flag", 0));
        if (openCnt != null && openCnt > 0) {
            return null;
        }
        if (StringUtils.isEmpty(card.getBizNo())) {
            card.setBizNo("ZG" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        }
        card.setStage(STAGE_ISSUE);
        card.setStatus(STATUS_ACTIVE);
        card.setCloseReceiptNo(null);
        card.setDelFlag(0);
        this.postRectifyCardMapper.insert(card);

        TPostRectifyStage head = newStage(card.getId(), STAGE_ISSUE, 0, ACTION_ISSUE,
                "核签单号" + card.getPunishBillNo() + "；地址" + card.getOutletAddr()
                        + "；责令改正期限" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(card.getRectifyDeadline()));
        this.postRectifyStageMapper.insert(head);
        return attachAndReckon(card);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostRectifyCard advance(Long id, String note) {
        return enterNext(id, note, null, null, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostRectifyCard advance(Long id, String note, Date finishAt) {
        return enterNext(id, note, finishAt, null, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostRectifyCard advance(Long id, String note, String closeReceiptNo) {
        return enterNext(id, note, null, closeReceiptNo, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostRectifyCard advance(Long id, String note, boolean halt) {
        return enterNext(id, note, null, null, halt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostRectifyCard rollback(Long id, String note) {
        return retreat(id, note, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostRectifyCard rollback(Long id, String note, Date finishAt) {
        return retreat(id, note, finishAt);
    }

    /**
     * 往前挪一格：落点唯一，只能从当前格落到相邻下一格。
     * 跳着过、在已封口的卡上过，一律不算过成（返回 null）。
     * halt=true 不是往前挪，而是在当前格点明“这一段不再往下走”，卡就地封口。
     */
    private TPostRectifyCard enterNext(Long id, String note, Date finishAt, String closeReceiptNo, boolean halt) {
        TPostRectifyCard card = this.postRectifyCardMapper.selectById(id);
        if (card == null || StringUtils.isEmpty(note)) {
            return null;
        }
        List<TPostRectifyStage> ledger = loadLedger(id);
        int current = reckonStage(ledger);
        // 已销案、已打住的卡，事后谁也不能再往它后面接一段；封口与否照履历末笔算，不靠屏上的号。
        if (current < STAGE_ISSUE || isSealed(ledger, card)) {
            return null;
        }
        if (halt) {
            // 就在当前段落一笔 HALT，段次不挪、卡打住；事后谁也不能再接段或收回。
            int layer = layerCount(ledger, current);
            this.postRectifyStageMapper.insert(newStage(id, current, layer, ACTION_HALT, note));
            card.setStage(current);
            card.setStatus(STATUS_HALT);
            this.postRectifyCardMapper.updateById(card);
            return attachAndReckon(card);
        }
        int target = current + 1;
        if (target > MAX_STAGE) {
            return null;
        }
        // 重载与落点必须对得上：带改完之日的只准落整改中，带回执的只准落销案，且销案必须有回执。
        if (finishAt != null && target != STAGE_RECTIFY) {
            return null;
        }
        if ((closeReceiptNo != null || target == STAGE_CLOSE)
                && (target != STAGE_CLOSE || StringUtils.isEmpty(closeReceiptNo))) {
            return null;
        }

        int layer = layerCount(ledger, target);
        TPostRectifyStage row = newStage(id, target, layer, ACTION_ENTER, note);
        if (target == STAGE_RECTIFY) {
            row.setFinishAt(finishAt);
        }
        if (target == STAGE_REVIEW) {
            // 复查当场点：期限过了还没报完成，或报来的完成日晚于期限。
            row.setOverdue(isOverdue(ledger, card.getRectifyDeadline()) ? 1 : 0);
        }
        this.postRectifyStageMapper.insert(row);

        // 屏上的号照履历算出来重写一遍——摆出来的号与算出来的号打架时，以算的为准。
        card.setStage(target);
        if (target == STAGE_CLOSE) {
            // 销案：各栏当场合上，备案名录那一行落个回执；还想改动只能另起一张新卡。
            card.setStatus(STATUS_CLOSED);
            card.setCloseReceiptNo(closeReceiptNo);
        } else {
            card.setStatus(STATUS_ACTIVE);
        }
        this.postRectifyCardMapper.updateById(card);
        return attachAndReckon(card);
    }

    /**
     * 往回收一段：目标段先前落过的名目压在底下（旧层原样留账、不再上屏），
     * 后补的一层另起；底下一层与后补的一层不许同屏并列。
     */
    private TPostRectifyCard retreat(Long id, String note, Date finishAt) {
        TPostRectifyCard card = this.postRectifyCardMapper.selectById(id);
        if (card == null || StringUtils.isEmpty(note)) {
            return null;
        }
        List<TPostRectifyStage> ledger = loadLedger(id);
        int current = reckonStage(ledger);
        // 封口与否照履历末笔算，不看屏上写的号；打住、销案的卡不得再往回收。
        if (current < STAGE_ISSUE || isSealed(ledger, card)) {
            return null;
        }
        if (current <= STAGE_ISSUE) {
            // 下达段之前无处可退。
            return null;
        }
        int target = current - 1;
        if (finishAt != null && target != STAGE_RECTIFY) {
            return null;
        }
        int layer = layerCount(ledger, target);
        TPostRectifyStage row = newStage(id, target, layer, ACTION_BACK, note);
        if (target == STAGE_RECTIFY) {
            row.setFinishAt(finishAt);
        }
        this.postRectifyStageMapper.insert(row);

        card.setStage(target);
        card.setStatus(STATUS_ACTIVE);
        this.postRectifyCardMapper.updateById(card);
        return attachAndReckon(card);
    }

    /** 卡是否已封口：履历末笔是打住，或已经过到销案。封口后任何过口都不再受理。 */
    private boolean isSealed(List<TPostRectifyStage> ledger, TPostRectifyCard card) {
        if (ledger == null || ledger.isEmpty()) {
            return Integer.valueOf(STATUS_CLOSED).equals(card.getStatus())
                    || Integer.valueOf(STATUS_HALT).equals(card.getStatus());
        }
        TPostRectifyStage last = ledger.get(ledger.size() - 1);
        return ACTION_HALT.equals(last.getAction())
                || Integer.valueOf(STAGE_CLOSE).equals(last.getStageNo());
    }

    /** 取整段履历，按过口先后排（同一时刻再按主键排）。 */
    private List<TPostRectifyStage> loadLedger(Long cardId) {
        return this.postRectifyStageMapper.selectList(new QueryWrapper<TPostRectifyStage>()
                .eq("card_id", cardId)
                .orderByAsc("action_time", "id"));
    }

    /**
     * 当前段照履历推算：最后一笔过口落到第几段，卡就停在第几段。
     * 一笔没有算未起（-1）。
     */
    private int reckonStage(List<TPostRectifyStage> ledger) {
        if (ledger == null || ledger.isEmpty()) {
            return -1;
        }
        TPostRectifyStage last = ledger.get(ledger.size() - 1);
        return last.getStageNo() == null ? -1 : last.getStageNo();
    }

    /** 该段已经落过几层，新进来的一层接着往下排。 */
    private int layerCount(List<TPostRectifyStage> ledger, int stageNo) {
        int count = 0;
        for (TPostRectifyStage row : ledger) {
            if (stageNo == (row.getStageNo() == null ? -1 : row.getStageNo())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 复查当场点逾期：整改中段当前层没报完成且已过期限，或报来的完成日晚于期限。
     */
    private boolean isOverdue(List<TPostRectifyStage> ledger, Date deadline) {
        if (deadline == null) {
            return false;
        }
        TPostRectifyStage rectify = null;
        for (TPostRectifyStage row : ledger) {
            if (Integer.valueOf(STAGE_RECTIFY).equals(row.getStageNo())) {
                rectify = row; // 履历按先后排，留到最后的是当前层
            }
        }
        Date finishAt = rectify == null ? null : rectify.getFinishAt();
        if (finishAt == null) {
            return new Date().after(deadline);
        }
        return finishAt.after(deadline);
    }

    /** 附履历并按推算结果摆正屏显段次；同段只把当前层（最大层）摆上屏，压底的旧层不并列。 */
    private TPostRectifyCard attachAndReckon(TPostRectifyCard card) {
        List<TPostRectifyStage> ledger = loadLedger(card.getId());
        card.setStages(currentView(ledger));
        int reckoned = reckonStage(ledger);
        if (reckoned >= 0) {
            card.setStage(reckoned);
        }
        return card;
    }

    /** 当前视图：每段只取最大一层（后补层在上屏，压底层留账不显示）。 */
    private List<TPostRectifyStage> currentView(List<TPostRectifyStage> ledger) {
        Map<Integer, TPostRectifyStage> top = new HashMap<Integer, TPostRectifyStage>();
        for (TPostRectifyStage row : ledger) {
            top.put(row.getStageNo(), row);
        }
        List<TPostRectifyStage> view = new ArrayList<TPostRectifyStage>(top.values());
        view.sort((a, b) -> a.getStageNo() - b.getStageNo());
        return view;
    }

    private TPostRectifyStage newStage(Long cardId, int stageNo, int layerNo, String action, String note) {
        TPostRectifyStage row = new TPostRectifyStage();
        row.setCardId(cardId);
        row.setStageNo(stageNo);
        row.setLayerNo(layerNo);
        row.setAction(action);
        row.setEntryNote(note);
        row.setOverdue(0);
        row.setActionTime(new Date());
        return row;
    }
}
