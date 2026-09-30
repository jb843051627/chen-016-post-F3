package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TPostRectifyCard;

/**
 * 责令整改事务卡 Service接口（state-machine 形状：单据流转，不提供增删改查全套）
 *
 * 一条整改事务线：一卡四段（下达/整改中/复查/销案），只能相邻过口，
 * 往前挪一格、往回退一格各自只有 advance / rollback 一处落点；
 * 需要别的用法，只在同一方法名下另并重载，不开当场造卡、提笔涂改、整张抹去的把手。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITPostRectifyCardService {

    /** 按主键查询单据（附整改履历，段次为系统按履历推算的权威值） */
    TPostRectifyCard selectTPostRectifyCardById(Long id);

    /** 列表查询（流转台账） */
    List<TPostRectifyCard> selectTPostRectifyCardList(QueryWrapper<TPostRectifyCard> queryWrapper);

    /**
     * 起卡：下达段落账（核签单钉号、网点地址、责令改正期限必填）。
     * 一家网点名下同时只容一张在办的卡；不合规返回 null。
     */
    TPostRectifyCard issue(TPostRectifyCard card);

    /**
     * 往前挪一格：过相邻下一段，落该段的记事，返回更新后的单据；被拒返回 null。
     * 入复查段时，期限已过仍未报完成（或报来的完成日晚于期限）当场点 overdue。
     */
    TPostRectifyCard advance(Long id, String note);

    /** 过到整改中段的重载：带企业报来的改完之日 */
    TPostRectifyCard advance(Long id, String note, Date finishAt);

    /** 过到销案段的重载：带备案名录落账回执号（无回执不得销案） */
    TPostRectifyCard advance(Long id, String note, String closeReceiptNo);

    /** 打住重载：halt=true 时在当前这一段点明“这一段不再往下走”，段次不挪，卡就地封口 */
    TPostRectifyCard advance(Long id, String note, boolean halt);

    /**
     * 往回收一段：目标段先前落过的名目压在底下（旧层原样保留），后补的一层另起，
     * 两层不许同屏并列；被拒返回 null。
     */
    TPostRectifyCard rollback(Long id, String note);

    /** 收回到整改中段的重载：带企业重新报来的改完之日 */
    TPostRectifyCard rollback(Long id, String note, Date finishAt);
}
