package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TPostRectifyCard;
import com.fc.v2.model.auto.TPostRectifyCardRecord;

/**
 * 责令整改事务卡 Service接口（state-machine 形状：单据流转，不提供增删改查全套）
 *
 * 四段只许挨着走：下达(0) → 整改中(1) → 复查(2) → 销案(3)；
 * 往前一格、往回一格各自只有 advance / rollback 一处落点，需要别的用法就在 advance 名下另并一个重载。
 * 不设当场造卡（open 之外无 insert）、提笔涂改（无 update 记事）、整张抹去（无 delete）的把手。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITPostRectifyCardService {

    /** 过口动作：正常进入下一段 */
    String ACTION_ENTER = "enter";
    /** 过口动作：本口点明“这一段不再往下走”，卡就此打住 */
    String ACTION_TERMINATE = "terminate";
    /** 过口动作：销案合卡 */
    String ACTION_CLOSE = "close";

    /** 按主键查询单据；当前段次不以卡上存的号为准，按在屏履历层数重算后覆盖返回 */
    TPostRectifyCard selectTPostRectifyCardById(Long id);

    /** 列表查询（流转台账）；每张卡的当前段次按履历重算 */
    List<TPostRectifyCard> selectTPostRectifyCardList(QueryWrapper<TPostRectifyCard> queryWrapper);

    /**
     * 起卡（下达段开卡）：核签单钉过的号、网点地址、责令改正期限随下达段写入，
     * 同一网点名下已有在办卡时拒起。返回新卡；被拒返回 null。
     */
    TPostRectifyCard open(String outletNo, String outletAddr, String sourceBillNo,
                          Date correctDeadline, String operator);

    /**
     * 往前挪一格：在屏履历推出当前停段 s，落第 s+1 段的层并返回更新后的卡。
     * 整改中段 content=企业报送的整改处数与完成日；复查段 content=现场核对结果
     * （期限过了仍未报送的当场点注）；挪入销案段当场合卡并在备案名录落回执。
     * 终局卡、跳段、同段不挪格再报一遍的，一律拒绝并返回 null。
     */
    TPostRectifyCard advance(Long id, String content, String operator);

    /**
     * advance 的另并用法：action 传 {@link #ACTION_TERMINATE} 时不挪格，
     * 在当前段点明“这一段不再往下走”，卡就此打住，事后谁也不能再接段。
     * 其余动作值一律拒绝。返回更新后的卡；被拒返回 null。
     */
    TPostRectifyCard advance(Long id, String action, String content, String operator);

    /**
     * 往回收一段重来：当前段在屏层原样压底（名目一个字不改，与后补新层不同屏并列），
     * 卡退回前一段；重新过时只在新进入的段另开一层。已在下达段、终局卡一律拒绝，返回 null。
     */
    TPostRectifyCard rollback(Long id, String operator);

    /** 整改履历在屏主线：按段次升序，从销案往回数到达下达，每段一笔 */
    List<TPostRectifyCardRecord> selectActiveTrace(Long cardId);

    /** 整改履历全部层（含压底旧层）：压底层只作留档，不与在屏层同屏 */
    List<TPostRectifyCardRecord> selectAllRecords(Long cardId);
}
