-- post 邮政业市场监管与快递末端服务监督 -- schema (chen-016)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_016

CREATE TABLE IF NOT EXISTS t_post_company_arch (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '经营企业在册底档字号',
  site_name varchar(128) DEFAULT NULL COMMENT '企业全称',
  site_type varchar(32) DEFAULT NULL COMMENT '许可类别',
  road_name varchar(128) DEFAULT NULL COMMENT '管辖邮政管理局(省—市—县)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '单卡日均件量低界(件/日)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '中界(件/日)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '高界(件/日)',
  status int DEFAULT NULL COMMENT '底档情形 0在册 1已注销',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='经营企业在册底档';

CREATE TABLE IF NOT EXISTS t_post_due_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '告知事项号',
  due_at datetime DEFAULT NULL COMMENT '该动手那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '提前几日开口告知',
  content varchar(255) DEFAULT NULL COMMENT '事由与被告知企业记要',
  status int DEFAULT NULL COMMENT '条目情形 0未开口 1已送达 2送不下去',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='经营许可效期与备案年度核验到期告知事项';

CREATE TABLE IF NOT EXISTS t_post_outlet_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '末端网点备案建档卡号',
  node_no int DEFAULT NULL COMMENT '本卡版本序',
  site_id int DEFAULT NULL COMMENT '所属企业在册底档',
  site_no varchar(64) DEFAULT NULL COMMENT '所属企业底档字号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡网点日均处理件数(件/日)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本卡报备营业面积(平方米)',
  content varchar(255) DEFAULT NULL COMMENT '核验串（随版本序走）',
  grade_level int DEFAULT NULL COMMENT '日均件量折到的服务序位',
  rectify_receipt varchar(64) DEFAULT NULL COMMENT '整改销案时落到本行的备案名录回执号',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已冻住',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='快递末端网点备案建档卡';

CREATE TABLE IF NOT EXISTS t_post_punish_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '查处核签单号',
  node_no int DEFAULT NULL COMMENT '当前所在开次 0..3',
  sign_mode int DEFAULT NULL COMMENT '同关并签办法 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本开应签人数',
  sign_count int DEFAULT NULL COMMENT '本开已签人数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='违法行为查处逐级核签单';

CREATE TABLE IF NOT EXISTS t_post_quality_rule (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '服务质量判据代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '低界达标率(百分比)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '中界达标率(百分比)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '高界达标率(百分比)',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '让线顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0现行 1已让位',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务质量达标率判据线';

CREATE TABLE IF NOT EXISTS t_post_rectify_card (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '责令整改事务卡号',
  outlet_no varchar(64) DEFAULT NULL COMMENT '网点号（一家网点同时只容一张在办的卡）',
  outlet_addr varchar(255) DEFAULT NULL COMMENT '网点地址（下达段写入）',
  source_bill_no varchar(64) DEFAULT NULL COMMENT '核签单上钉过的号（查处核签单号，下达段写入）',
  correct_deadline datetime DEFAULT NULL COMMENT '责令改正期限（下达段写入）',
  review_result varchar(255) DEFAULT NULL COMMENT '现场点验结果（复查段写入）',
  stage int DEFAULT NULL COMMENT '当前段次镜像，0..3（下达/整改中/复查/销案）；不作数，以履历行数推算为准',
  status int DEFAULT NULL COMMENT '卡的落点 0未起 1在办 2已销案 3已打住',
  content varchar(255) DEFAULT NULL COMMENT '整改记事（整改中段企业报送，镜像履历当层内容）',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除（已销案/已打住的卡不允许删除）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rectify_biz_no (biz_no),
  KEY idx_rectify_outlet (outlet_no, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='责令整改事务卡';

-- 整改履历：一段一层，同段只认第一遍；回退重来时旧层压底（buried=1，内容原样留档），
-- 与新层不许同屏并列：每卡每段至多一层在屏（active_slot=0），DB 唯一索引兜底。
CREATE TABLE IF NOT EXISTS t_post_rectify_card_record (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint NOT NULL COMMENT '所属整改事务卡',
  stage int NOT NULL COMMENT '段次 0下达 1整改中 2复查 3销案',
  layer_no int NOT NULL COMMENT '本段层序，第一遍为0，回退后重来逐层加一',
  active_slot int DEFAULT NULL COMMENT '在屏层为1，压底层为NULL；NULL不进唯一约束，故每卡每段至多一层在屏',
  content varchar(500) DEFAULT NULL COMMENT '本层记事（该段经手人落笔的名目，压底后一字不改）',
  operator varchar(64) DEFAULT NULL COMMENT '过口经手人',
  action varchar(64) DEFAULT NULL COMMENT '过口动作 enter/terminate/close',
  buried int NOT NULL DEFAULT 0 COMMENT '是否已被回退压底 0否 1是',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除（履历一律不删）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间（过口当日时刻）',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rectify_record_active (card_id, stage, active_slot),
  KEY idx_rectify_record_card (card_id, stage, layer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='责令整改事务卡整改履历（一段一层，压底不并列）';

CREATE TABLE IF NOT EXISTS t_post_report_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '月度业务报表报送批次号',
  row_no int DEFAULT NULL COMMENT '原报表行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被挑出的前置项代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行报送业务量(万件)',
  status int DEFAULT NULL COMMENT '行落地情形 0待核 1已收 2挂起',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月度业务报表核收行';

-- 初始底档数据（在册、已注销两类各一条）
-- t_post_company_arch 两条种子底档：id=0 在册、id=1 已注销。
-- 单卡日均件数三线上限取 30/90/240 件（等于上限算高一序位）。
INSERT IGNORE INTO t_post_company_arch (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'YZ00', '青屏市在册企业底档（青屏速递有限公司，在册）', '省内经营', '青屏省—青屏市—青屏县', 30.00, 90.00, 240.00, 0, 0, 'seed', NOW()),
       (1, 'YZ01', '青屏市在册企业底档（云驿快递服务有限公司，已注销）', '省内经营', '青屏省—青屏市—青屏县', 30.00, 90.00, 240.00, 1, 0, 'seed', NOW());

