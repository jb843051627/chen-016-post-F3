-- 仅供测试：H2(MySQL 模式) 下的最小表结构，列与实体契约对齐
CREATE TABLE t_post_rectify_card (
  id bigint NOT NULL,
  biz_no varchar(64),
  outlet_no varchar(64),
  outlet_addr varchar(255),
  source_bill_no varchar(64),
  correct_deadline datetime,
  review_result varchar(255),
  stage int,
  status int,
  content varchar(255),
  last_action varchar(64),
  del_flag int DEFAULT 0,
  create_by varchar(64),
  create_time datetime,
  update_by varchar(64),
  update_time datetime,
  remark varchar(500),
  PRIMARY KEY (id),
  UNIQUE (biz_no)
);

CREATE TABLE t_post_rectify_card_record (
  id bigint NOT NULL,
  card_id bigint NOT NULL,
  stage int NOT NULL,
  layer_no int NOT NULL,
  active_slot int,
  content varchar(500),
  operator varchar(64),
  action varchar(64),
  buried int NOT NULL DEFAULT 0,
  del_flag int DEFAULT 0,
  create_by varchar(64),
  create_time datetime,
  update_by varchar(64),
  update_time datetime,
  remark varchar(500),
  PRIMARY KEY (id),
  UNIQUE (card_id, stage, active_slot)
);

CREATE TABLE t_post_outlet_card (
  id bigint NOT NULL,
  bill_no varchar(64),
  node_no int,
  site_id int,
  site_no varchar(64),
  qty decimal(12,2),
  fine_amt decimal(12,2),
  content varchar(255),
  grade_level int,
  rectify_receipt varchar(64),
  status int,
  del_flag int DEFAULT 0,
  create_by varchar(64),
  create_time datetime,
  update_by varchar(64),
  update_time datetime,
  remark varchar(500),
  PRIMARY KEY (id)
);
