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
 * 整改事务卡段履历对象 t_post_rectify_stage
 * 只追加，不涂改，不删除；同一段收回重来时旧层压底，当前视图只取最大层。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_post_rectify_stage")
@ApiModel(value = "TPostRectifyStage", description = "整改事务卡段履历")
public class TPostRectifyStage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属整改事务卡 */
    @TableField("card_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属整改事务卡")
    private Long cardId;

    /** 段次 0..3（下达/整改中/复查/销案） */
    @TableField("stage_no")
    @ApiModelProperty(value = "段次 0..3（下达/整改中/复查/销案）")
    private Integer stageNo;

    /** 本段内第几层 0起，收回重来只增不改 */
    @TableField("layer_no")
    @ApiModelProperty(value = "本段内第几层 0起，收回重来只增不改")
    private Integer layerNo;

    /** 过口名目 ISSUE起卡 ENTER过段 BACK收回 HALT打住 */
    @TableField("action")
    @ApiModelProperty(value = "过口名目 ISSUE起卡 ENTER过段 BACK收回 HALT打住")
    private String action;

    /** 当段落账记事（一处只落一遍） */
    @TableField("entry_note")
    @ApiModelProperty(value = "当段落账记事（一处只落一遍）")
    private String entryNote;

    /** 整改中段：企业报来的改完之日 */
    @TableField("finish_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "整改中段：企业报来的改完之日")
    private Date finishAt;

    /** 复查段：期限过了仍未报完成 0否 1当场点出 */
    @TableField("overdue")
    @ApiModelProperty(value = "复查段：期限过了仍未报完成 0否 1当场点出")
    private Integer overdue;

    /** 过口时刻 */
    @TableField("action_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "过口时刻")
    private Date actionTime;

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

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Integer getStageNo() {
        return stageNo;
    }

    public void setStageNo(Integer stageNo) {
        this.stageNo = stageNo;
    }

    public Integer getLayerNo() {
        return layerNo;
    }

    public void setLayerNo(Integer layerNo) {
        this.layerNo = layerNo;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntryNote() {
        return entryNote;
    }

    public void setEntryNote(String entryNote) {
        this.entryNote = entryNote;
    }

    public Date getFinishAt() {
        return finishAt;
    }

    public void setFinishAt(Date finishAt) {
        this.finishAt = finishAt;
    }

    public Integer getOverdue() {
        return overdue;
    }

    public void setOverdue(Integer overdue) {
        this.overdue = overdue;
    }

    public Date getActionTime() {
        return actionTime;
    }

    public void setActionTime(Date actionTime) {
        this.actionTime = actionTime;
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
