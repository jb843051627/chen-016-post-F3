package com.fc.v2.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TPostOutletCardMapper;
import com.fc.v2.mapper.auto.TPostRectifyCardMapper;
import com.fc.v2.mapper.auto.TPostRectifyCardRecordMapper;
import com.fc.v2.model.auto.TPostOutletCard;
import com.fc.v2.model.auto.TPostRectifyCard;
import com.fc.v2.model.auto.TPostRectifyCardRecord;
import com.fc.v2.service.ITPostRectifyCardService;
import com.fc.v2.util.DateUtils;
import com.fc.v2.util.SnowflakeIdWorker;

/**
 * 责令整改事务卡 Service业务层处理（state-machine 形状：单据流转）
 *
 * 卡停在第几段，只按在屏履历的层数推算（在屏层数 - 1），卡上的 stage 只是镜像；
 * 往前一格、往回一格各自只有 advance / rollback 一处落点，没有造卡、涂改、抹卡的把手。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TPostRectifyCardServiceImpl implements ITPostRectifyCardService {

    /** 段次：下达 */
    private static final int STAGE_ISSUE = 0;
    /** 段次：整改中 */
    private static final int STAGE_RECTIFYING = 1;
    /** 段次：复查 */
    private static final int STAGE_REVIEW = 2;
    /** 段次：销案 */
    private static final int STAGE_CLOSED = 3;

    /** 卡的落点：在办 */
    private static final int STATUS_ACTIVE = 1;
    /** 卡的落点：已销案 */
    private static final int STATUS_CLOSED = 2;
    /** 卡的落点：已打住 */
    private static final int STATUS_TERMINATED = 3;

    /** 履历层在屏 */
    private static final int SLOT_ON = 1;
    /** 已压底 */
    private static final int BURIED_YES = 1;
    private static final int BURIED_NO = 0;

    private static final String CARD_NO_PREFIX = "ZG";

    @javax.annotation.Resource
    private TPostRectifyCardMapper postRectifyCardMapper;

    @javax.annotation.Resource
    private TPostRectifyCardRecordMapper postRectifyCardRecordMapper;

    @javax.annotation.Resource
    private TPostOutletCardMapper postOutletCardMapper;

    @Override
    public TPostRectifyCard selectTPostRectifyCardById(Long id) {
        TPostRectifyCard r = this.postRectifyCardMapper.selectById(id);
        return r == null ? null : reconcileStage(r);
    }

    @Override
    public List<TPostRectifyCard> selectTPostRectifyCardList(QueryWrapper<TPostRectifyCard> queryWrapper) {
        List<TPostRectifyCard> list = this.postRectifyCardMapper.selectList(queryWrapper);
        for (TPostRectifyCard r : list) {
            reconcileStage(r);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostRectifyCard open(String outletNo, String outletAddr, String sourceBillNo,
                                 Date correctDeadline, String operator) {
        if (isBlank(outletNo) || isBlank(sourceBillNo) || correctDeadline == null) {
            return null;
        }
        // 一家网点名下同时只容一张在办的卡：头一张没销案也没打住之前，第二张立不起来
        Integer openCnt = this.postRectifyCardMapper.selectCount(new QueryWrapper<TPostRectifyCard>()
                .eq("outlet_no", outletNo)
                .eq("status", STATUS_ACTIVE)
                .eq("del_flag", 0));
        if (openCnt != null && openCnt > 0) {
            return null;
        }

        TPostRectifyCard card = new TPostRectifyCard();
        card.setBizNo(CARD_NO_PREFIX + SnowflakeIdWorker.getUUID());
        card.setOutletNo(outletNo);
        card.setOutletAddr(outletAddr);
        card.setSourceBillNo(sourceBillNo);
        card.setCorrectDeadline(correctDeadline);
        card.setStage(STAGE_ISSUE);
        card.setStatus(STATUS_ACTIVE);
        card.setLastAction(ACTION_ENTER);
        card.setDelFlag(0);
        this.postRectifyCardMapper.insert(card);

        // 下达段第一层：钉过的号、地址、期限随下达写入
        TPostRectifyCardRecord rec = newRecord(card.getId(), STAGE_ISSUE, 0,
                "核签单号:" + sourceBillNo + "；网点地址:" + nullToEmpty(outletAddr)
                        + "；责令改正期限:" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, correctDeadline),
                operator, ACTION_ENTER);
        this.postRectifyCardRecordMapper.insert(rec);
        return card;
    }

    @Override
    public TPostRectifyCard advance(Long id, String content, String operator) {
        return advance(id, ACTION_ENTER, content, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostRectifyCard advance(Long id, String action, String content, String operator) {
        TPostRectifyCard card = loadActive(id);
        if (card == null || isBlank(action)) {
            return null;
        }

        int stage = currentStage(card.getId());
        if (stage < STAGE_ISSUE) {
            return null;
        }

        // 不挪格的过口：当前段点明“这一段不再往下走”，卡就此打住
        if (ACTION_TERMINATE.equals(action)) {
            return terminate(card, stage, content, operator);
        }
        if (!ACTION_ENTER.equals(action)) {
            return null;
        }

        // 挨着走：只许挪一格，不许多跳
        int next = stage + 1;
        if (next > STAGE_CLOSED) {
            return null;
        }
        // 整改中、复查两段都要有该段经手人落笔的名目；销案是合卡，不带新记事
        if (next != STAGE_CLOSED && isBlank(content)) {
            return null;
        }

        int layerNo = layerCount(card.getId(), next);
        String layerContent;
        if (next == STAGE_RECTIFYING) {
            layerContent = "企业报送整改：" + content;
        } else if (next == STAGE_REVIEW) {
            // 期限过了还没报的，当场点出来；正常的记现场核对结果
            String overdue = card.getCorrectDeadline() != null
                    && new Date().after(card.getCorrectDeadline()) ? "（已过责令改正期限，当场点注）" : "";
            layerContent = "现场点验结果：" + content + overdue;
        } else {
            layerContent = "销案合卡，备案名录回执号：" + card.getBizNo();
        }
        TPostRectifyCardRecord rec = newRecord(card.getId(), next, layerNo, layerContent, operator,
                next == STAGE_CLOSED ? ACTION_CLOSE : ACTION_ENTER);
        this.postRectifyCardRecordMapper.insert(rec);

        card.setStage(next);
        card.setLastAction(next == STAGE_CLOSED ? ACTION_CLOSE : ACTION_ENTER);
        if (next == STAGE_RECTIFYING) {
            card.setContent(content);
        } else if (next == STAGE_REVIEW) {
            card.setReviewResult(content);
        }
        if (next == STAGE_CLOSED) {
            // 走到销案各栏当场合上；顺带在备案名录那一行落回执
            card.setStatus(STATUS_CLOSED);
            this.postOutletCardMapper.update(null, new UpdateWrapper<TPostOutletCard>()
                    .eq("bill_no", card.getOutletNo())
                    .eq("del_flag", 0)
                    .set("rectify_receipt", card.getBizNo()));
        }
        this.postRectifyCardMapper.updateById(card);
        return reconcileStage(card);
    }

    /**
     * 往回收一段重来（只退一格）：从停段 S 退到 S-1，退出的那一段 S 在屏层原样压底
     * （名目一字不改，active_slot 置 NULL，与后补新层不许同屏并列），再往前过时该段另开新层；
     * 收回段（前一段）先前落过的名目不动。已在下达段、终局卡一律拒绝，返回 null。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostRectifyCard rollback(Long id, String operator) {
        TPostRectifyCard card = loadActive(id);
        if (card == null) {
            return null;
        }
        int stage = currentStage(card.getId());
        // 已在下达段无处可退；终局卡（销案/打住）根本进不到这里
        if (stage <= STAGE_ISSUE) {
            return null;
        }

        // 被撤回的那一段在屏层压底：名目一字不改，重走时只许另开新层
        buryActiveLayer(card.getId(), stage);

        card.setStage(stage - 1);
        card.setStatus(STATUS_ACTIVE);
        card.setLastAction("rollback");
        // 被撤回段的镜像记事收起，等重走时随新层再落
        if (stage == STAGE_REVIEW) {
            card.setReviewResult(null);
        } else if (stage == STAGE_RECTIFYING) {
            card.setContent(null);
        }
        this.postRectifyCardMapper.updateById(card);
        return reconcileStage(card);
    }

    /** 把某段在屏层压底（active_slot 置 NULL、buried=1），名目内容一字不动 */
    private void buryActiveLayer(Long cardId, int stage) {
        this.postRectifyCardRecordMapper.update(null, new UpdateWrapper<TPostRectifyCardRecord>()
                .eq("card_id", cardId)
                .eq("stage", stage)
                .eq("active_slot", SLOT_ON)
                .eq("del_flag", 0)
                .set("active_slot", null)
                .set("buried", BURIED_YES));
    }

    @Override
    public List<TPostRectifyCardRecord> selectActiveTrace(Long cardId) {
        return this.postRectifyCardRecordMapper.selectList(new QueryWrapper<TPostRectifyCardRecord>()
                .eq("card_id", cardId)
                .eq("active_slot", SLOT_ON)
                .eq("del_flag", 0)
                .orderByAsc("stage"));
    }

    @Override
    public List<TPostRectifyCardRecord> selectAllRecords(Long cardId) {
        return this.postRectifyCardRecordMapper.selectList(new QueryWrapper<TPostRectifyCardRecord>()
                .eq("card_id", cardId)
                .eq("del_flag", 0)
                .orderByAsc("stage", "layer_no"));
    }

    /**
     * 打住：不再往履历上多落一句（同段第二遍账上只认先前那一遍），
     * 只把卡头置为终局 status=3，打住缘由落在 remark；
     * 事后 advance / rollback 一律拒（loadActive 只放在办卡）。
     */
    private TPostRectifyCard terminate(TPostRectifyCard card, int stage, String content, String operator) {
        card.setStatus(STATUS_TERMINATED);
        card.setLastAction(ACTION_TERMINATE);
        card.setRemark("第" + stage + "段点明不再往下走" + (isBlank(content) ? "" : "：" + content)
                + (isBlank(operator) ? "" : "（" + operator + "）"));
        this.postRectifyCardMapper.updateById(card);
        return reconcileStage(card);
    }

    /** 只取在办卡；销了案、打了住的卡一律不得再过口 */
    private TPostRectifyCard loadActive(Long id) {
        if (id == null) {
            return null;
        }
        TPostRectifyCard card = this.postRectifyCardMapper.selectById(id);
        if (card == null || card.getDelFlag() == null || card.getDelFlag() != 0) {
            return null;
        }
        if (card.getStatus() == null || card.getStatus() != STATUS_ACTIVE) {
            return null;
        }
        return card;
    }

    /** 当前停段：照着在屏履历的层数推算，屏上写的号、承办人认的号都当不得真 */
    private int currentStage(Long cardId) {
        Integer activeLayers = this.postRectifyCardRecordMapper.selectCount(
                new QueryWrapper<TPostRectifyCardRecord>()
                        .eq("card_id", cardId)
                        .eq("active_slot", SLOT_ON)
                        .eq("del_flag", 0));
        int n = activeLayers == null ? 0 : activeLayers;
        return n - 1;
    }

    /** 该段已经落过几层（含压底旧层），决定新一层的层序 */
    private int layerCount(Long cardId, int stage) {
        Integer cnt = this.postRectifyCardRecordMapper.selectCount(new QueryWrapper<TPostRectifyCardRecord>()
                .eq("card_id", cardId)
                .eq("stage", stage)
                .eq("del_flag", 0));
        return cnt == null ? 0 : cnt;
    }

    /**
     * 摆出来的号跟算出来的号打架时以算的为准：用履历推算值覆盖卡上的 stage 镜像。
     */
    private TPostRectifyCard reconcileStage(TPostRectifyCard card) {
        int computed = currentStage(card.getId());
        if (computed >= STAGE_ISSUE && (card.getStage() == null || card.getStage() != computed)) {
            card.setStage(computed);
        }
        return card;
    }

    private TPostRectifyCardRecord newRecord(Long cardId, int stage, int layerNo,
                                             String content, String operator, String action) {
        TPostRectifyCardRecord rec = new TPostRectifyCardRecord();
        rec.setCardId(cardId);
        rec.setStage(stage);
        rec.setLayerNo(layerNo);
        rec.setActiveSlot(SLOT_ON);
        rec.setContent(content);
        rec.setOperator(operator);
        rec.setAction(action);
        rec.setBuried(BURIED_NO);
        rec.setDelFlag(0);
        return rec;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
