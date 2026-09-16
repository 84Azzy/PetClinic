USE petclinic;

-- 演示账号密码均为 123456；仅用于本地学习环境。
INSERT INTO sys_user(id,username,password_hash,display_name,phone,email,account_type,status,token_version) VALUES
(1,'admin','$2a$10$QRNmcpMAXwRReRFmDkCvy.y/ObWLAHTCMPE6UB7nZZcRT9vADtj7e','系统管理员','13800000001','admin@petclinic.local','ADMIN','ACTIVE',1),
(2,'staff','$2a$10$QRNmcpMAXwRReRFmDkCvy.y/ObWLAHTCMPE6UB7nZZcRT9vADtj7e','前台员工','13800000002','staff@petclinic.local','STAFF','ACTIVE',1),
(3,'owner_a','$2a$10$QRNmcpMAXwRReRFmDkCvy.y/ObWLAHTCMPE6UB7nZZcRT9vADtj7e','张女士','13800000003','zhang@example.com','OWNER','ACTIVE',1),
(4,'owner_b','$2a$10$QRNmcpMAXwRReRFmDkCvy.y/ObWLAHTCMPE6UB7nZZcRT9vADtj7e','李先生','13800000004','li@example.com','OWNER','ACTIVE',1);
INSERT INTO sys_role(id,code,name,description,status) VALUES (1,'ADMIN','管理员','全部管理权限','ACTIVE'),(2,'STAFF','员工','诊所日常业务','ACTIVE'),(3,'OWNER','宠物主人','个人宠物和预约','ACTIVE');
INSERT INTO sys_permission(id,parent_id,code,name,type,path,icon,sort_order,status) VALUES
(1,NULL,'dashboard:read','数据看板','MENU','/dashboard','DataAnalysis',10,'ACTIVE'),
(2,NULL,'owner:manage','主人管理','MENU','/owners','User',20,'ACTIVE'),(3,NULL,'pet:manage','宠物管理','MENU','/pets','MostlyCloudy',30,'ACTIVE'),
(4,NULL,'vet:manage','兽医管理','MENU','/vets','Avatar',40,'ACTIVE'),(5,NULL,'schedule:manage','排班管理','MENU','/schedules','Calendar',50,'ACTIVE'),
(6,NULL,'visit:manage','预约管理','MENU','/visits','Tickets',60,'ACTIVE'),(7,NULL,'medical:manage','病历管理','MENU','/medical-records','Document',70,'ACTIVE'),
(8,NULL,'vaccination:manage','疫苗管理','MENU','/vaccinations','FirstAidKit',80,'ACTIVE'),(9,NULL,'notice:manage','公告管理','MENU','/notices','Bell',90,'ACTIVE'),
(10,NULL,'feedback:manage','反馈管理','MENU','/feedback','ChatLineSquare',100,'ACTIVE'),(11,NULL,'system:manage','系统管理','MENU','/system/users','Setting',110,'ACTIVE'),
(12,NULL,'ai:chat','AI 助手','MENU','/ai','MagicStick',120,'ACTIVE'),(13,3,'pet:create','新增宠物','BUTTON',NULL,NULL,1,'ACTIVE'),
(14,3,'pet:update','修改宠物','BUTTON',NULL,NULL,2,'ACTIVE'),(15,3,'pet:delete','停用宠物','BUTTON',NULL,NULL,3,'ACTIVE'),
(16,6,'visit:create','创建预约','BUTTON',NULL,NULL,1,'ACTIVE'),(17,6,'visit:cancel','取消预约','BUTTON',NULL,NULL,2,'ACTIVE');
INSERT INTO sys_user_role VALUES (1,1),(2,2),(3,3),(4,3);
INSERT INTO sys_role_permission SELECT 1,id FROM sys_permission;
-- 不恰当：STAFF 只有 pet:manage 菜单权限，却没有 Controller 要求的新增、修改、停用动作权限。
-- INSERT INTO sys_role_permission(role_id,permission_id) VALUES (2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),(2,8),(2,9),(2,10),...;
INSERT INTO sys_role_permission(role_id,permission_id) VALUES (2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),(2,8),(2,9),(2,10),(2,13),(2,14),(2,15),(3,3),(3,5),(3,6),(3,8),(3,9),(3,10),(3,12),(3,13),(3,14),(3,15),(3,16),(3,17);

INSERT INTO owner(id,user_id,name,phone,email,address,status) VALUES (1,3,'张女士','13800000003','zhang@example.com','上海市浦东新区云台路 18 号','ACTIVE'),(2,4,'李先生','13800000004','li@example.com','上海市徐汇区桂林路 66 号','ACTIVE');
INSERT INTO pet_type(id,name,description,status) VALUES (1,'猫','家猫及常见猫科宠物','ACTIVE'),(2,'犬','各类家养犬','ACTIVE'),(3,'兔','家兔和垂耳兔','ACTIVE'),(4,'其他','鸟类、仓鼠等小型宠物','ACTIVE');
INSERT INTO pet(id,owner_id,type_id,name,gender,breed,birth_date,color,microchip_no,allergies,status) VALUES
(1,1,1,'糯米','FEMALE','英短','2022-04-12','蓝白','MC20220001','青霉素','ACTIVE'),(2,1,2,'可乐','MALE','柯基','2021-09-03','黄白','MC20210002',NULL,'ACTIVE'),(3,2,1,'元宝','MALE','橘猫','2023-01-20','橘色','MC20230003',NULL,'ACTIVE'),(4,2,3,'团团','FEMALE','垂耳兔','2024-05-08','白色',NULL,NULL,'ACTIVE');
INSERT INTO specialty(id,name,description,status) VALUES (1,'全科','常规检查与常见疾病','ACTIVE'),(2,'外科','手术及创伤处理','ACTIVE'),(3,'皮肤科','皮肤和毛发疾病','ACTIVE'),(4,'口腔科','牙齿与口腔疾病','ACTIVE'),(5,'影像科','X 光与超声诊断','ACTIVE');
INSERT INTO vet(id,name,phone,email,license_no,biography,status) VALUES
(1,'王医生','13900000001','wang@petclinic.local','VET-SH-1001','十年小动物全科经验','ACTIVE'),(2,'陈医生','13900000002','chen@petclinic.local','VET-SH-1002','擅长软组织外科与骨科','ACTIVE'),(3,'周医生','13900000003','zhou@petclinic.local','VET-SH-1003','专注宠物皮肤病与过敏','ACTIVE'),(4,'赵医生','13900000004','zhao@petclinic.local','VET-SH-1004','口腔及影像联合诊疗','ACTIVE');
INSERT INTO vet_specialty VALUES (1,1),(2,1),(2,2),(3,1),(3,3),(4,4),(4,5);
INSERT INTO vet_schedule_slot(id,vet_id,start_time,end_time,status,note) VALUES
(1,1,'2026-08-22 09:00:00','2026-08-22 09:30:00','BOOKED','上午门诊'),(2,1,'2026-08-22 09:30:00','2026-08-22 10:00:00','AVAILABLE','上午门诊'),
(3,2,'2026-08-22 14:00:00','2026-08-22 14:30:00','BOOKED','外科门诊'),(4,2,'2026-08-22 14:30:00','2026-08-22 15:00:00','AVAILABLE','外科门诊'),
(5,3,'2026-08-23 10:00:00','2026-08-23 10:30:00','AVAILABLE','皮肤科'),(6,4,'2026-08-23 15:00:00','2026-08-23 15:30:00','AVAILABLE','口腔科');
-- 2026-09-14 之后的演示排班。
INSERT INTO vet_schedule_slot(vet_id,start_time,end_time,status,note) VALUES
(1,'2026-09-15 09:00:00','2026-09-15 09:30:00','AVAILABLE','全科上午门诊'),
(1,'2026-09-15 09:30:00','2026-09-15 10:00:00','AVAILABLE','全科上午门诊'),
(2,'2026-09-15 14:00:00','2026-09-15 14:30:00','AVAILABLE','外科下午门诊'),
(2,'2026-09-15 14:30:00','2026-09-15 15:00:00','AVAILABLE','外科下午门诊'),
(3,'2026-09-16 10:00:00','2026-09-16 10:30:00','AVAILABLE','皮肤科门诊'),
(3,'2026-09-16 10:30:00','2026-09-16 11:00:00','AVAILABLE','皮肤科门诊'),
(4,'2026-09-16 15:00:00','2026-09-16 15:30:00','AVAILABLE','口腔科门诊'),
(4,'2026-09-16 15:30:00','2026-09-16 16:00:00','AVAILABLE','影像联合门诊'),
(1,'2026-09-17 09:00:00','2026-09-17 09:30:00','AVAILABLE','全科上午门诊'),
(2,'2026-09-17 14:00:00','2026-09-17 14:30:00','AVAILABLE','外科下午门诊'),
(3,'2026-09-18 10:00:00','2026-09-18 10:30:00','AVAILABLE','皮肤科门诊'),
(4,'2026-09-18 15:00:00','2026-09-18 15:30:00','AVAILABLE','口腔科门诊');
INSERT INTO visit(id,pet_id,slot_id,vet_id,created_by,request_id,reason,status,created_at) VALUES
(1,1,1,1,3,'DEMO-REQ-001','近期食欲下降，需要常规检查','COMPLETED','2026-08-20 10:00:00'),(2,3,3,2,4,'DEMO-REQ-002','后腿轻微跛行','SCHEDULED','2026-08-21 09:30:00');
INSERT INTO medical_record(visit_id,pet_id,vet_id,symptoms,diagnosis,treatment,prescription,notes) VALUES (1,1,1,'食欲下降、精神一般','轻度胃肠不适','调整饮食并观察三天','益生菌每日一次','如持续呕吐立即复诊');
INSERT INTO vaccination_record(pet_id,vaccine_name,batch_no,vaccinated_date,next_due_date,veterinarian,status,notes) VALUES
(1,'猫三联','CAT-2026-018','2026-03-10','2027-03-10','王医生','VALID','年度加强针'),(2,'犬六联','DOG-2026-022','2026-02-15','2027-02-15','陈医生','VALID','无不良反应'),(3,'狂犬疫苗','RAB-2025-901','2025-07-01','2026-07-01','王医生','OVERDUE','需要尽快补种');
INSERT INTO notice(title,content,status,publisher_id,published_at,expires_at,sort_order) VALUES
('夏季防暑提醒','高温天气请避免中午遛狗，并确保宠物有充足饮水。','PUBLISHED',1,'2026-08-15 09:00:00','2026-09-30 23:59:59',10),
('周末门诊安排','本周六正常接诊，周日下午仅开放急诊。','PUBLISHED',2,'2026-08-20 12:00:00','2026-08-24 23:59:59',20);
INSERT INTO feedback(user_id,category,title,content,contact,status,reply,replied_by,replied_at) VALUES
(3,'SERVICE','候诊区建议','希望候诊区增加猫犬分区。','13800000003','REPLIED','感谢建议，我们正在调整候诊区布局。',2,'2026-08-20 16:00:00'),
(4,'SYSTEM','预约提醒','建议增加预约前一天提醒。','li@example.com','PENDING',NULL,NULL,NULL);
