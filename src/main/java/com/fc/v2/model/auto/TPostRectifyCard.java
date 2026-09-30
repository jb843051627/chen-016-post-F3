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

    /** 网点号（一家网点同时只容一张在办的卡） */
    @TableField("outlet_no")
    @ApiModelProperty(value = "网点号")
    private String outletNo;

    /** 网点地址（下达段写入） */
    @TableField("outlet_addr")
    @ApiModelProperty(value = "网点地址")
    private String outletAddr;

    /** 核签单上钉过的号（查处核签单号，下达段写入） */
    @TableField("source_bill_no")
    @ApiModelProperty(value = "核签单上钉过的号")
    private String sourceBillNo;

    /** 责令改正期限（下达段写入） */
    @TableField("correct_deadline")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "责令改正期限")
    private Date correctDeadline;

    /** 现场点验结果（复查段写入） */
    @TableField("review_result")
    @ApiModelProperty(value = "现场点验结果")
    private String reviewResult;

    /** 当前段次镜像 0..3（下达/整改中/复查/销案）；不作数，以履历行数推算为准 */
    @TableField("stage")
    @ApiModelProperty(value = "当前段次镜像，以履历推算为准")
    private Integer stage;

    /** 卡的落点 0未起 1在办 2已销案 3已打住 */
    @TableField("status")
    @ApiModelProperty(value = "卡的落点 0未起 1在办 2已销案 3已打住")
    private Integer status;

    /** 整改记事（整改中段企业报送，镜像履历当层内容） */
    @TableField("content")
    @ApiModelProperty(value = "整改记事")
    private String content;

    /** 最近一次过口动作 */
    @TableField("last_action")
    @ApiModelProperty(value = "最近一次过口动作")
    private String lastAction;

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

    public String getOutletAddr() {
        return outletAddr;
    }

    public void setOutletAddr(String outletAddr) {
        this.outletAddr = outletAddr;
    }

    public String getSourceBillNo() {
        return sourceBillNo;
    }

    public void setSourceBillNo(String sourceBillNo) {
        this.sourceBillNo = sourceBillNo;
    }

    public Date getCorrectDeadline() {
        return correctDeadline;
    }

    public void setCorrectDeadline(Date correctDeadline) {
        this.correctDeadline = correctDeadline;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(String reviewResult) {
        this.reviewResult = reviewResult;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
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
}
