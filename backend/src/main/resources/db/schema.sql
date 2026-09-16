CREATE DATABASE IF NOT EXISTS petclinic
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE petclinic;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_message;
DROP TABLE IF EXISTS ai_conversation;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS vaccination_record;
DROP TABLE IF EXISTS medical_record;
DROP TABLE IF EXISTS visit;
DROP TABLE IF EXISTS vet_schedule_slot;
DROP TABLE IF EXISTS vet_specialty;
DROP TABLE IF EXISTS vet;
DROP TABLE IF EXISTS specialty;
DROP TABLE IF EXISTS pet;
DROP TABLE IF EXISTS pet_type;
DROP TABLE IF EXISTS owner;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(100),
  account_type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  token_version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_sys_user_username UNIQUE (username),
  INDEX idx_sys_user_status_type (status, account_type)
) ENGINE = InnoDB COMMENT = '系统用户表';

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_sys_role_code UNIQUE (code)
) ENGINE = InnoDB COMMENT = '系统角色表';

CREATE TABLE sys_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT,
  code VARCHAR(100) NOT NULL,
  name VARCHAR(80) NOT NULL,
  type VARCHAR(20) NOT NULL,
  path VARCHAR(160),
  icon VARCHAR(60),
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_sys_permission_code UNIQUE (code),
  CONSTRAINT fk_permission_parent FOREIGN KEY (parent_id) REFERENCES sys_permission (id)
) ENGINE = InnoDB COMMENT = '系统权限表';

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE = InnoDB COMMENT = '用户角色关联表';

CREATE TABLE sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
  CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE = InnoDB COMMENT = '角色权限关联表';

CREATE TABLE owner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT ,
  name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  email VARCHAR(100),
  address VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_owner_user UNIQUE (user_id),
  CONSTRAINT fk_owner_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  INDEX idx_owner_name_phone (name, phone)
) ENGINE = InnoDB COMMENT = '宠物主人档案表';

CREATE TABLE pet_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_pet_type_name UNIQUE (name)
) ENGINE = InnoDB COMMENT = '宠物类型表';

CREATE TABLE pet (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_id BIGINT NOT NULL,
  type_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  gender VARCHAR(20),
  breed VARCHAR(80),
  birth_date DATE,
  color VARCHAR(50),
  microchip_no VARCHAR(80),
  allergies VARCHAR(500),
  photo_url VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_pet_owner FOREIGN KEY (owner_id) REFERENCES owner (id),
  CONSTRAINT fk_pet_type FOREIGN KEY (type_id) REFERENCES pet_type (id),
  CONSTRAINT uk_pet_microchip UNIQUE (microchip_no),
  INDEX idx_pet_owner_status (owner_id, status)
) ENGINE = InnoDB COMMENT = '宠物档案表';

CREATE TABLE specialty (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_specialty_name UNIQUE (name)
) ENGINE = InnoDB COMMENT = '兽医专科表';

CREATE TABLE vet (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(100),
  license_no VARCHAR(80) NOT NULL,
  biography VARCHAR(1000),
  avatar_url VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_vet_license UNIQUE (license_no),
  INDEX idx_vet_name_status (name, status)
) ENGINE = InnoDB COMMENT = '兽医信息表';

CREATE TABLE vet_specialty (
  vet_id BIGINT NOT NULL,
  specialty_id BIGINT NOT NULL,
  PRIMARY KEY (vet_id, specialty_id),
  CONSTRAINT fk_vs_vet FOREIGN KEY (vet_id) REFERENCES vet (id),
  CONSTRAINT fk_vs_specialty FOREIGN KEY (specialty_id) REFERENCES specialty (id)
) ENGINE = InnoDB COMMENT = '兽医专科关联表';

CREATE TABLE vet_schedule_slot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vet_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  note VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_vet_slot UNIQUE (vet_id, start_time),
  CONSTRAINT fk_slot_vet FOREIGN KEY (vet_id) REFERENCES vet (id),
  INDEX idx_slot_available (vet_id, status, start_time)
) ENGINE = InnoDB COMMENT = '兽医排班时段表';

CREATE TABLE visit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pet_id BIGINT NOT NULL,
  slot_id BIGINT NOT NULL,
  vet_id BIGINT NOT NULL,
  created_by BIGINT NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
  cancelled_at DATETIME,
  cancel_reason VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_visit_request UNIQUE (request_id),
  CONSTRAINT fk_visit_pet FOREIGN KEY (pet_id) REFERENCES pet (id),
  CONSTRAINT fk_visit_slot FOREIGN KEY (slot_id) REFERENCES vet_schedule_slot (id),
  CONSTRAINT fk_visit_vet FOREIGN KEY (vet_id) REFERENCES vet (id),
  CONSTRAINT fk_visit_user FOREIGN KEY (created_by) REFERENCES sys_user (id),
  INDEX idx_visit_slot (slot_id),
  INDEX idx_visit_owner_status (created_by, status, id),
  INDEX idx_visit_vet_status (vet_id, status)
) ENGINE = InnoDB COMMENT = '预约就诊记录表';

CREATE TABLE medical_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  visit_id BIGINT NOT NULL,
  pet_id BIGINT NOT NULL,
  vet_id BIGINT NOT NULL,
  symptoms TEXT NOT NULL,
  diagnosis TEXT NOT NULL,
  treatment TEXT NOT NULL,
  prescription TEXT,
  notes TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_record_visit UNIQUE (visit_id),
  CONSTRAINT fk_record_visit FOREIGN KEY (visit_id) REFERENCES visit (id),
  CONSTRAINT fk_record_pet FOREIGN KEY (pet_id) REFERENCES pet (id),
  CONSTRAINT fk_record_vet FOREIGN KEY (vet_id) REFERENCES vet (id),
  INDEX idx_record_pet (pet_id, id)
) ENGINE = InnoDB COMMENT = '诊疗记录表';

CREATE TABLE vaccination_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pet_id BIGINT NOT NULL,
  vaccine_name VARCHAR(100) NOT NULL,
  batch_no VARCHAR(80),
  vaccinated_date DATE NOT NULL,
  next_due_date DATE,
  veterinarian VARCHAR(80),
  status VARCHAR(20) NOT NULL DEFAULT 'VALID',
  notes VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_vaccine_pet FOREIGN KEY (pet_id) REFERENCES pet (id),
  INDEX idx_vaccine_due (next_due_date, status),
  INDEX idx_vaccine_pet (pet_id, vaccinated_date)
) ENGINE = InnoDB COMMENT = '疫苗接种记录表';

CREATE TABLE notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  publisher_id BIGINT,
  published_at DATETIME,
  expires_at DATETIME,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES sys_user (id),
  INDEX idx_notice_active (status, expires_at, sort_order)
) ENGINE = InnoDB COMMENT = '公告信息表';

CREATE TABLE feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category VARCHAR(30) NOT NULL,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  contact VARCHAR(100),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reply VARCHAR(1000),
  replied_by BIGINT,
  replied_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_feedback_replier FOREIGN KEY (replied_by) REFERENCES sys_user (id),
  INDEX idx_feedback_status (status, id)
) ENGINE = InnoDB COMMENT = '用户反馈表';

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50),
  module VARCHAR(80),
  operation VARCHAR(80),
  http_method VARCHAR(10),
  request_uri VARCHAR(255),
  response_status INT,
  duration_ms BIGINT,
  ip_address VARCHAR(64),
  error_message VARCHAR(1000),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_log_user_time (user_id, created_at)
) ENGINE = InnoDB COMMENT = '系统操作日志表';

CREATE TABLE ai_conversation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(120),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  INDEX idx_ai_conversation_user (user_id, id)
) ENGINE = InnoDB COMMENT = 'AI 助手会话表';

CREATE TABLE ai_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  tool_name VARCHAR(100),
  tool_payload JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_message_conversation
    FOREIGN KEY (conversation_id)
    REFERENCES ai_conversation (id)
    ON DELETE CASCADE,
  INDEX idx_ai_message_conversation (conversation_id, id)
) ENGINE = InnoDB COMMENT = 'AI 助手消息表';
