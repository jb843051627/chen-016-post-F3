package com.fc.v2.model.auto;

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
 * 责令整改事务卡整改履历对象 t_post_rectify_card_record
 *
 * 一段一层，同段第二遍进来只认先前那一遍；回退重来时旧层压底（buried=1），
 * 与后补的新层不许同屏并列（active_slot：在屏1，压底NULL，唯一索引兜底）。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_post_rectify_card_record")
@ApiModel(value = "TPostRectifyCardRecord", description = "责令整改事务卡整改履历")
public class TPostRectifyCardRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属整改事务卡 */
    @TableField("card_id")
    @ApiModelProperty(value = "所属整改事务卡")
    private Long cardId;

    /** 段次 0下达 1整改中 2复查 3销案 */
    @TableField("stage")
    @ApiModelProperty(value = "段次 0下达 1整改中 2复查 3销案")
    private Integer stage;

    /** 本段层序，第一遍为0，回退后重来逐层加一 */
    @TableField("layer_no")
    @ApiModelProperty(value = "本段层序")
    private Integer layerNo;

    /** 在屏层为1，压底层为NULL；每卡每段至多一层在屏 */
    @TableField("active_slot")
    @ApiModelProperty(value = "在屏层1，压底NULL")
    private Integer activeSlot;

    /** 本层记事（该段经手人落笔的名目，压底后一字不改） */
    @TableField("content")
    @ApiModelProperty(value = "本层记事")
    private String content;

    /** 过口经手人 */
    @TableField("operator")
    @ApiModelProperty(value = "过口经手人")
    private String operator;

    /** 过口动作 enter/terminate/close */
    @TableField("action")
    @ApiModelProperty(value = "过口动作 enter/terminate/close")
    private String action;

    /** 是否已被回退压底 0否 1是 */
    @TableField("buried")
    @ApiModelProperty(value = "是否已被回退压底")
    private Integer buried;

    /** 删除标记 0正常 1删除（履历一律不删） */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记")
    private Integer delFlag;

    /** 创建者 */
    @TableField("create_by")
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间（过口当日时刻） */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField("update_by")
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField("update_time")
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

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getLayerNo() {
        return layerNo;
    }

    public void setLayerNo(Integer layerNo) {
        this.layerNo = layerNo;
    }

    public Integer getActiveSlot() {
        return activeSlot;
    }

    public void setActiveSlot(Integer activeSlot) {
        this.activeSlot = activeSlot;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getBuried() {
        return buried;
    }

    public void setBuried(Integer buried) {
        this.buried = buried;
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
