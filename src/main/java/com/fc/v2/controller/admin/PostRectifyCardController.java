package com.fc.v2.controller.admin;

import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TPostRectifyCard;
import com.fc.v2.service.ITPostRectifyCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 责令整改事务卡 Controller（state-machine 形状：流转入口）
 *
 * 只有起卡、往前过一格、往回收一格三个入口；不提供当场造卡之外的增、改、删把手。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "责令整改事务卡")
@Controller
@RequestMapping("/postRectifyCard")
public class PostRectifyCardController extends BaseController {

    private final String prefix = "admin/postRectifyCard";

    @Autowired
    private ITPostRectifyCardService postRectifyCardService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("postRectifyCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "责令整改事务卡流转台账", action = "list")
    @ApiOperation(value = "流转台账", notes = "流转台账")
    @GetMapping("/list")
    @RequiresPermissions("postRectifyCard:list")
    @ResponseBody
    public ResultTable list(TPostRectifyCard record) {
        QueryWrapper<TPostRectifyCard> queryWrapper = new QueryWrapper<TPostRectifyCard>();
        startPage();
        com.github.pagehelper.PageInfo<TPostRectifyCard> page =
                new com.github.pagehelper.PageInfo<TPostRectifyCard>(postRectifyCardService.selectTPostRectifyCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "责令整改事务卡起卡", action = "issue")
    @ApiOperation(value = "起卡（下达段落账）", notes = "核签单钉号、网点地址、责令改正期限")
    @PostMapping("/issue")
    @RequiresPermissions("postRectifyCard:issue")
    @ResponseBody
    public AjaxResult issue(TPostRectifyCard record) {
        return toAjax(postRectifyCardService.issue(record) != null ? 1 : 0);
    }

    @Log(title = "责令整改事务卡推进", action = "advance")
    @ApiOperation(value = "往前过一格", notes = "只能过相邻下一段；finishAt 用于整改中，closeReceiptNo 用于销案，halt=true 在本段打住")
    @PostMapping("/advance")
    @RequiresPermissions("postRectifyCard:advance")
    @ResponseBody
    public AjaxResult advance(Long id, String note,
                              @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date finishAt,
                              String closeReceiptNo, Boolean halt) {
        TPostRectifyCard r;
        if (Boolean.TRUE.equals(halt)) {
            r = postRectifyCardService.advance(id, note, true);
        } else if (closeReceiptNo != null && !closeReceiptNo.trim().isEmpty()) {
            r = postRectifyCardService.advance(id, note, closeReceiptNo.trim());
        } else if (finishAt != null) {
            r = postRectifyCardService.advance(id, note, finishAt);
        } else {
            r = postRectifyCardService.advance(id, note);
        }
        return toAjax(r != null ? 1 : 0);
    }

    @Log(title = "责令整改事务卡回退", action = "rollback")
    @ApiOperation(value = "往回收一格", notes = "目标段旧层压底、新层另起，两层不同屏；finishAt 用于收回到整改中重报完成日")
    @PostMapping("/rollback")
    @RequiresPermissions("postRectifyCard:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String note,
                               @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date finishAt) {
        TPostRectifyCard r = finishAt != null
                ? postRectifyCardService.rollback(id, note, finishAt)
                : postRectifyCardService.rollback(id, note);
        return toAjax(r != null ? 1 : 0);
    }
}
