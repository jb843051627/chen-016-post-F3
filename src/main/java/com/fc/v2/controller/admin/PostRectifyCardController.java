package com.fc.v2.controller.admin;

import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TPostRectifyCard;
import com.fc.v2.service.ITPostRectifyCardService;
import com.fc.v2.util.DateUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 责令整改事务卡 Controller（state-machine 形状：流转入口）
 *
 * 只有起卡、往前挪（advance）、往回收（rollback）、履历查询入口；
 * 没有当场造卡之外的新增、提笔涂改、整张抹去的入口。
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

    @Log(title = "责令整改事务卡起卡", action = "open")
    @ApiOperation(value = "起卡（下达段开卡）", notes = "钉过的号、网点地址、责令改正期限随下达写入")
    @PostMapping("/open")
    @RequiresPermissions("postRectifyCard:open")
    @ResponseBody
    public AjaxResult open(String outletNo, String outletAddr, String sourceBillNo,
                           String correctDeadline, String operator) {
        if (correctDeadline == null || correctDeadline.trim().isEmpty()) {
            return error("责令改正期限必填，未起卡");
        }
        Date deadline = DateUtils.dateTime(DateUtils.YYYY_MM_DD_HH_MM_SS, correctDeadline.trim());
        TPostRectifyCard card = postRectifyCardService.open(outletNo, outletAddr, sourceBillNo, deadline, operator);
        // 挪没挪成只看服务那一回应给回什么：null 即被拒
        return card == null ? error("该网点名下尚有在办的卡，或下达要件不齐，未起卡") : AjaxResult.success(200, "起卡成功", card);
    }

    @Log(title = "责令整改事务卡推进", action = "advance")
    @ApiOperation(value = "往前挪一格", notes = "整改中段报整改处数与完成日，复查段报现场核对结果，再挪即销案")
    @PostMapping("/advance")
    @RequiresPermissions("postRectifyCard:advance")
    @ResponseBody
    public AjaxResult advance(Long id, String content, String operator) {
        TPostRectifyCard card = postRectifyCardService.advance(id, content, operator);
        return card == null ? error("不许跳段/重复落记，或卡已销案、已打住") : AjaxResult.success(200, "推进成功", card);
    }

    @Log(title = "责令整改事务卡打住", action = "terminate")
    @ApiOperation(value = "本口点明不再往下走", notes = "卡就此打住，事后谁也不能再接段")
    @PostMapping("/terminate")
    @RequiresPermissions("postRectifyCard:terminate")
    @ResponseBody
    public AjaxResult terminate(Long id, String content, String operator) {
        TPostRectifyCard card = postRectifyCardService.advance(id,
                ITPostRectifyCardService.ACTION_TERMINATE, content, operator);
        return card == null ? error("卡不在办，打住不成") : AjaxResult.success(200, "已打住", card);
    }

    @Log(title = "责令整改事务卡回退", action = "rollback")
    @ApiOperation(value = "往回收一段重来", notes = "当前段在屏层压底，名目原样留档，不与新层同屏并列")
    @PostMapping("/rollback")
    @RequiresPermissions("postRectifyCard:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String operator) {
        TPostRectifyCard card = postRectifyCardService.rollback(id, operator);
        return card == null ? error("已在下达段无处可退，或卡已销案、已打住") : AjaxResult.success(200, "回退成功", card);
    }

    @Log(title = "责令整改事务卡履历", action = "trace")
    @ApiOperation(value = "整改履历（在屏主线）", notes = "从销案往回数到达下达，每段一笔")
    @GetMapping("/trace")
    @RequiresPermissions("postRectifyCard:list")
    @ResponseBody
    public AjaxResult trace(Long id) {
        return AjaxResult.success(200, "操作成功", postRectifyCardService.selectActiveTrace(id));
    }

    @Log(title = "责令整改事务卡履历全层", action = "traceAll")
    @ApiOperation(value = "整改履历全部层（含压底旧层）", notes = "压底层只作留档，不与在屏层同屏")
    @GetMapping("/traceAll")
    @RequiresPermissions("postRectifyCard:list")
    @ResponseBody
    public AjaxResult traceAll(Long id) {
        return AjaxResult.success(200, "操作成功", postRectifyCardService.selectAllRecords(id));
    }
}
