package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 责令整改事务卡对象 t_post_rectify_card
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_post_rectify_card")
@ApiModel(value = "TPostRectifyCard", description = "责令整改事务卡")
public class TPostRectifyCard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 责令整改事务卡号 */
    @TableField("biz_no")
    @ApiModelProperty(value = "责令整改事务卡号")
    private String bizNo;

    /** 网点字号（同网点在办唯一） */
    @TableField("outlet_no")
    @ApiModelProperty(value = "网点字号（同网点在办唯一）")
    private String outletNo;

    /** 下达段：核签单上钉过的号 */
    @TableField("punish_bill_no")
    @ApiModelProperty(value = "下达段：核签单上钉过的号")
    private String punishBillNo;

    /** 下达段：网点地址 */
    @TableField("outlet_addr")
    @ApiModelProperty(value = "下达段：网点地址")
    private String outletAddr;

    /** 下达段：责令改正期限止点 */
    @TableField("rectify_deadline")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "下达段：责令改正期限止点")
    private Date rectifyDeadline;

    /** 屏显段次 0..3（下达/整改中/复查/销案），权威值以履历推算为准 */
    @TableField("stage")
    @ApiModelProperty(value = "屏显段次 0..3（下达/整改中/复查/销案），权威值以履历推算为准")
    private Integer stage;

    /** 卡的落点 1在办 2已销案 3已打住 */
    @TableField("status")
    @ApiModelProperty(value = "卡的落点 1在办 2已销案 3已打住")
    private Integer status;

    /** 销案段：备案名录落账回执号 */
    @TableField("close_receipt_no")
    @ApiModelProperty(value = "销案段：备案名录落账回执号")
    private String closeReceiptNo;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 整改履历（非表列）：段段追加，当前段由其推算 */
    @TableField(exist = false)
    @ApiModelProperty(value = "整改履历")
    private List<TPostRectifyStage> stages;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public String getOutletNo() {
        return outletNo;
    }

    public void setOutletNo(String outletNo) {
        this.outletNo = outletNo;
    }

    public String getPunishBillNo() {
        return punishBillNo;
    }

    public void setPunishBillNo(String punishBillNo) {
        this.punishBillNo = punishBillNo;
    }

    public String getOutletAddr() {
        return outletAddr;
    }

    public void setOutletAddr(String outletAddr) {
        this.outletAddr = outletAddr;
    }

    public Date getRectifyDeadline() {
        return rectifyDeadline;
    }

    public void setRectifyDeadline(Date rectifyDeadline) {
        this.rectifyDeadline = rectifyDeadline;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCloseReceiptNo() {
        return closeReceiptNo;
    }

    public void setCloseReceiptNo(String closeReceiptNo) {
        this.closeReceiptNo = closeReceiptNo;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<TPostRectifyStage> getStages() {
        return stages;
    }

    public void setStages(List<TPostRectifyStage> stages) {
        this.stages = stages;
    }
}
