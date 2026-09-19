/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80025
 Source Host           : localhost:3306
 Source Schema         : petclinic

 Target Server Type    : MySQL
 Target Server Version : 80025
 File Encoding         : 65001

 Date: 16/09/2026 19:45:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_conversation
-- ----------------------------
DROP TABLE IF EXISTS `ai_conversation`;
CREATE TABLE `ai_conversation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_conversation_user_status_updated`(`user_id` ASC, `status` ASC, `updated_at` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_ai_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_conversation
-- ----------------------------
INSERT INTO `ai_conversation` VALUES (1, 3, '请查询我的宠物，只需简要列出名字。', 'ACTIVE', '2026-09-11 11:53:10', '2026-09-11 11:53:58');
INSERT INTO `ai_conversation` VALUES (2, 3, '查询我的预约，只需简要说明预约状态。', 'ACTIVE', '2026-09-11 11:58:51', '2026-09-14 16:38:25');
INSERT INTO `ai_conversation` VALUES (3, 3, '查询我的宠物', 'DELETED', '2026-09-11 15:02:08', '2026-09-11 15:02:08');
INSERT INTO `ai_conversation` VALUES (4, 4, '明天下午有哪些医生可以就诊', 'DELETED', '2026-09-14 16:22:43', '2026-09-14 16:22:55');
INSERT INTO `ai_conversation` VALUES (5, 4, '给我的猫预约明天下午陈医生，原因是后腿轻微跛行', 'DELETED', '2026-09-14 16:23:28', '2026-09-14 16:23:28');
INSERT INTO `ai_conversation` VALUES (6, 3, '给我的猫约明天下午的陈医生', 'DELETED', '2026-09-14 16:29:02', '2026-09-14 16:29:02');
INSERT INTO `ai_conversation` VALUES (7, 3, '给我的猫约明天下午的陈医生', 'DELETED', '2026-09-14 16:34:59', '2026-09-14 16:34:59');
INSERT INTO `ai_conversation` VALUES (8, 4, '给我的猫约明天下午的医生', 'ACTIVE', '2026-09-14 16:47:05', '2026-09-14 16:48:04');

-- ----------------------------
-- Table structure for ai_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_message`;
CREATE TABLE `ai_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tool_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `tool_payload` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_message_conversation`(`conversation_id` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_ai_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_message
-- ----------------------------
INSERT INTO `ai_message` VALUES (1, 1, 'USER', '请查询我的宠物，只需简要列出名字。', NULL, NULL, '2026-09-11 11:53:10', '2026-09-11 11:53:10');
INSERT INTO `ai_message` VALUES (2, 1, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 8, \"resultCount\": 2}', '2026-09-11 11:53:10', '2026-09-11 11:53:10');
INSERT INTO `ai_message` VALUES (3, 1, 'ASSISTANT', '您的宠物有：糯米、可乐。', NULL, NULL, '2026-09-11 11:53:10', '2026-09-11 11:53:10');
INSERT INTO `ai_message` VALUES (4, 1, 'USER', '刚才列出的第一只宠物叫什么？', NULL, NULL, '2026-09-11 11:53:58', '2026-09-11 11:53:58');
INSERT INTO `ai_message` VALUES (5, 1, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 2, \"resultCount\": 2}', '2026-09-11 11:53:58', '2026-09-11 11:53:58');
INSERT INTO `ai_message` VALUES (6, 1, 'ASSISTANT', '第一只宠物叫糯米。', NULL, NULL, '2026-09-11 11:53:58', '2026-09-11 11:53:58');
INSERT INTO `ai_message` VALUES (7, 2, 'USER', '查询我的预约，只需简要说明预约状态。', NULL, NULL, '2026-09-11 11:58:51', '2026-09-11 11:58:51');
INSERT INTO `ai_message` VALUES (8, 2, 'TOOL', '工具调用成功', 'listMyVisits', '{\"name\": \"listMyVisits\", \"success\": true, \"durationMs\": 6, \"resultCount\": 1}', '2026-09-11 11:58:51', '2026-09-11 11:58:51');
INSERT INTO `ai_message` VALUES (9, 2, 'ASSISTANT', '您共有1条预约记录：宠物近期食欲下降的常规检查，状态为已完成。', NULL, NULL, '2026-09-11 11:58:51', '2026-09-11 11:58:51');
INSERT INTO `ai_message` VALUES (10, 3, 'USER', '查询我的宠物', NULL, NULL, '2026-09-11 15:02:08', '2026-09-11 15:02:08');
INSERT INTO `ai_message` VALUES (11, 3, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 4, \"resultCount\": 2}', '2026-09-11 15:02:08', '2026-09-11 15:02:08');
INSERT INTO `ai_message` VALUES (12, 3, 'ASSISTANT', '您当前有两只宠物：\n1. 糯米（英短，母，编号1）\n2. 可乐（柯基，公，编号2）\n请问需要为哪只宠物预约就诊？', NULL, NULL, '2026-09-11 15:02:08', '2026-09-11 15:02:08');
INSERT INTO `ai_message` VALUES (13, 2, 'USER', '你好', NULL, NULL, '2026-09-14 16:07:41', '2026-09-14 16:07:41');
INSERT INTO `ai_message` VALUES (14, 2, 'ASSISTANT', '你好！我是宠物诊所的AI助手，可以帮您预约就诊、查询预约或宠物信息。请问有什么可以帮您的？', NULL, NULL, '2026-09-14 16:07:41', '2026-09-14 16:07:41');
INSERT INTO `ai_message` VALUES (15, 2, 'USER', '明天下午有哪些医生可以就诊', NULL, NULL, '2026-09-14 16:08:14', '2026-09-14 16:08:14');
INSERT INTO `ai_message` VALUES (16, 2, 'TOOL', '工具调用成功', 'findVets', '{\"name\": \"findVets\", \"success\": true, \"durationMs\": 6, \"resultCount\": 4}', '2026-09-14 16:08:14', '2026-09-14 16:08:14');
INSERT INTO `ai_message` VALUES (17, 2, 'TOOL', '工具调用成功', 'findAvailableSlots', '{\"name\": \"findAvailableSlots\", \"success\": true, \"durationMs\": 4, \"resultCount\": 2}', '2026-09-14 16:08:14', '2026-09-14 16:08:14');
INSERT INTO `ai_message` VALUES (18, 2, 'TOOL', '工具调用成功', 'findAvailableSlots', '{\"name\": \"findAvailableSlots\", \"success\": true, \"durationMs\": 2, \"resultCount\": 2}', '2026-09-14 16:08:14', '2026-09-14 16:08:14');
INSERT INTO `ai_message` VALUES (19, 2, 'TOOL', '工具调用成功', 'findAvailableSlots', '{\"name\": \"findAvailableSlots\", \"success\": true, \"durationMs\": 2, \"resultCount\": 0}', '2026-09-14 16:08:14', '2026-09-14 16:08:14');
INSERT INTO `ai_message` VALUES (20, 2, 'TOOL', '工具调用成功', 'findAvailableSlots', '{\"name\": \"findAvailableSlots\", \"success\": true, \"durationMs\": 1, \"resultCount\": 0}', '2026-09-14 16:08:14', '2026-09-14 16:08:14');
INSERT INTO `ai_message` VALUES (21, 2, 'ASSISTANT', '明天（2026-09-15）下午只有陈医生有可预约时段，时间为14:00-14:30和14:30-15:00。王医生仅有上午时段，周医生和赵医生暂无空档。', NULL, NULL, '2026-09-14 16:08:14', '2026-09-14 16:08:14');
INSERT INTO `ai_message` VALUES (22, 4, 'USER', '明天下午有哪些医生可以就诊', NULL, NULL, '2026-09-14 16:22:43', '2026-09-14 16:22:43');
INSERT INTO `ai_message` VALUES (23, 4, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 79, \"resultCount\": 1}', '2026-09-14 16:22:43', '2026-09-14 16:22:43');
INSERT INTO `ai_message` VALUES (24, 4, 'ASSISTANT', '明天（2026-09-15）下午可以就诊的医生如下：\n陈医生：14:00-14:30、14:30-15:00', NULL, NULL, '2026-09-14 16:22:43', '2026-09-14 16:22:43');
INSERT INTO `ai_message` VALUES (25, 4, 'USER', '给我的猫预约明天下午陈医生', NULL, NULL, '2026-09-14 16:22:55', '2026-09-14 16:22:55');
INSERT INTO `ai_message` VALUES (26, 4, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 10, \"resultCount\": 2}', '2026-09-14 16:22:55', '2026-09-14 16:22:55');
INSERT INTO `ai_message` VALUES (27, 4, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 15, \"resultCount\": 1}', '2026-09-14 16:22:55', '2026-09-14 16:22:55');
INSERT INTO `ai_message` VALUES (28, 4, 'ASSISTANT', '已为您找到猫咪「元宝」。陈医生明天（2026-09-15）下午的可预约时段有：14:00-14:30、14:30-15:00。请问猫咪的就诊原因是什么呢？', NULL, NULL, '2026-09-14 16:22:55', '2026-09-14 16:22:55');
INSERT INTO `ai_message` VALUES (29, 5, 'USER', '给我的猫预约明天下午陈医生，原因是后腿轻微跛行', NULL, NULL, '2026-09-14 16:23:28', '2026-09-14 16:23:28');
INSERT INTO `ai_message` VALUES (30, 5, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 3, \"resultCount\": 2}', '2026-09-14 16:23:28', '2026-09-14 16:23:28');
INSERT INTO `ai_message` VALUES (31, 5, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 13, \"resultCount\": 1}', '2026-09-14 16:23:28', '2026-09-14 16:23:28');
INSERT INTO `ai_message` VALUES (32, 5, 'TOOL', '工具调用成功', 'findAvailableSlots', '{\"name\": \"findAvailableSlots\", \"success\": true, \"durationMs\": 6, \"resultCount\": 2}', '2026-09-14 16:23:28', '2026-09-14 16:23:28');
INSERT INTO `ai_message` VALUES (33, 5, 'ASSISTANT', '已为您查询到：您的猫“元宝”（橘猫）符合条件。陈医生明天（2026-09-15）下午可预约的时段有两个：\n1. 14:00 - 14:30\n2. 14:30 - 15:00\n请问您想预约哪一个具体时段？确认后我会为您生成预约草稿。', NULL, NULL, '2026-09-14 16:23:28', '2026-09-14 16:23:28');
INSERT INTO `ai_message` VALUES (34, 6, 'USER', '给我的猫约明天下午的陈医生', NULL, NULL, '2026-09-14 16:29:02', '2026-09-14 16:29:02');
INSERT INTO `ai_message` VALUES (35, 6, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 7, \"resultCount\": 3}', '2026-09-14 16:29:02', '2026-09-14 16:29:02');
INSERT INTO `ai_message` VALUES (36, 6, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 75, \"resultCount\": 1}', '2026-09-14 16:29:02', '2026-09-14 16:29:02');
INSERT INTO `ai_message` VALUES (37, 6, 'ASSISTANT', '您名下有两只在册的猫咪：糯米和sx，请问想为哪一只预约？另外请告知本次就诊的原因，以便我为您生成预约草稿。', NULL, NULL, '2026-09-14 16:29:02', '2026-09-14 16:29:02');
INSERT INTO `ai_message` VALUES (38, 7, 'USER', '给我的猫约明天下午的陈医生', NULL, NULL, '2026-09-14 16:34:59', '2026-09-14 16:34:59');
INSERT INTO `ai_message` VALUES (39, 7, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 7, \"resultCount\": 3}', '2026-09-14 16:34:59', '2026-09-14 16:34:59');
INSERT INTO `ai_message` VALUES (40, 7, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 73, \"resultCount\": 1}', '2026-09-14 16:34:59', '2026-09-14 16:34:59');
INSERT INTO `ai_message` VALUES (41, 7, 'ASSISTANT', '已查到陈医生明天（2026-09-15）下午的可预约时段：\n1. 14:00 - 14:30（外科下午门诊）\n2. 14:30 - 15:00（外科下午门诊）\n\n不过还需要确认两件事：\n1. 您名下有两只猫（糯米、sx），请问是哪一只？\n2. 请问这次就诊的原因是什么？\n\n确认后我再帮您整理预约草稿。', NULL, NULL, '2026-09-14 16:34:59', '2026-09-14 16:34:59');
INSERT INTO `ai_message` VALUES (42, 2, 'USER', '给我的猫约明天下午的陈医生', NULL, NULL, '2026-09-14 16:36:59', '2026-09-14 16:36:59');
INSERT INTO `ai_message` VALUES (43, 2, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 6, \"resultCount\": 3}', '2026-09-14 16:36:59', '2026-09-14 16:36:59');
INSERT INTO `ai_message` VALUES (44, 2, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 11, \"resultCount\": 1}', '2026-09-14 16:36:59', '2026-09-14 16:36:59');
INSERT INTO `ai_message` VALUES (45, 2, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 3, \"resultCount\": 3}', '2026-09-14 16:36:59', '2026-09-14 16:36:59');
INSERT INTO `ai_message` VALUES (46, 2, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 8, \"resultCount\": 1}', '2026-09-14 16:36:59', '2026-09-14 16:36:59');
INSERT INTO `ai_message` VALUES (47, 2, 'TOOL', '工具调用成功', 'findVets', '{\"name\": \"findVets\", \"success\": true, \"durationMs\": 6, \"resultCount\": 4}', '2026-09-14 16:36:59', '2026-09-14 16:36:59');
INSERT INTO `ai_message` VALUES (48, 2, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 7, \"resultCount\": 1}', '2026-09-14 16:36:59', '2026-09-14 16:36:59');
INSERT INTO `ai_message` VALUES (49, 2, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 2, \"resultCount\": 3}', '2026-09-14 16:36:59', '2026-09-14 16:36:59');
INSERT INTO `ai_message` VALUES (50, 2, 'ASSISTANT', '已查询到 2026-09-15（下午）的可预约时段：\n- 陈医生：14:00-14:30、14:30-15:00\n为了生成预约草稿，请补充：具体是哪只宠物（糯米、sx）；选择一个具体时段；就诊原因。', NULL, NULL, '2026-09-14 16:36:59', '2026-09-14 16:36:59');
INSERT INTO `ai_message` VALUES (51, 2, 'USER', '14:00-14:30  sx，腿受伤', NULL, NULL, '2026-09-14 16:38:25', '2026-09-14 16:38:25');
INSERT INTO `ai_message` VALUES (52, 2, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 5, \"resultCount\": 3}', '2026-09-14 16:38:25', '2026-09-14 16:38:25');
INSERT INTO `ai_message` VALUES (53, 2, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 6, \"resultCount\": 1}', '2026-09-14 16:38:25', '2026-09-14 16:38:25');
INSERT INTO `ai_message` VALUES (54, 2, 'ASSISTANT', '已为您生成预约草稿：宠物sx（猫，ID:5），陈医生，2026-09-15 14:00-14:30（slotId:10），就诊原因：腿受伤。等待用户确认，尚未创建预约。', NULL, '{\"petId\": 5, \"reason\": \"腿受伤\", \"slotId\": 10, \"summary\": \"宠物sx（猫）预约陈医生 2026-09-15 14:00-14:30，原因：腿受伤\"}', '2026-09-14 16:38:25', '2026-09-14 16:38:25');
INSERT INTO `ai_message` VALUES (55, 8, 'USER', '给我的猫约明天下午的医生', NULL, NULL, '2026-09-14 16:47:05', '2026-09-14 16:47:05');
INSERT INTO `ai_message` VALUES (56, 8, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 2, \"resultCount\": 2}', '2026-09-14 16:47:05', '2026-09-14 16:47:05');
INSERT INTO `ai_message` VALUES (57, 8, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 9, \"resultCount\": 1}', '2026-09-14 16:47:05', '2026-09-14 16:47:05');
INSERT INTO `ai_message` VALUES (58, 8, 'ASSISTANT', '已为您找到猫咪「元宝」。明天（2026-09-15）下午可预约的医生是陈医生（外科），可预约时段为：14:00-14:30、14:30-15:00。请问您带元宝就诊的原因是什么呢？', NULL, NULL, '2026-09-14 16:47:05', '2026-09-14 16:47:05');
INSERT INTO `ai_message` VALUES (59, 8, 'USER', '腿受伤', NULL, NULL, '2026-09-14 16:47:37', '2026-09-14 16:47:37');
INSERT INTO `ai_message` VALUES (60, 8, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 2, \"resultCount\": 2}', '2026-09-14 16:47:37', '2026-09-14 16:47:37');
INSERT INTO `ai_message` VALUES (61, 8, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 5, \"resultCount\": 1}', '2026-09-14 16:47:37', '2026-09-14 16:47:37');
INSERT INTO `ai_message` VALUES (62, 8, 'ASSISTANT', '您家元宝的腿受伤了，需要尽快就诊。明天（2026-09-15）下午陈医生（外科）还有两个可预约时段：\n1. 14:00-14:30\n2. 14:30-15:00\n\n请问您想约哪个时段呢？', NULL, NULL, '2026-09-14 16:47:37', '2026-09-14 16:47:37');
INSERT INTO `ai_message` VALUES (63, 8, 'USER', '14:00', NULL, NULL, '2026-09-14 16:48:04', '2026-09-14 16:48:04');
INSERT INTO `ai_message` VALUES (64, 8, 'TOOL', '工具调用成功', 'listMyPets', '{\"name\": \"listMyPets\", \"success\": true, \"durationMs\": 6, \"resultCount\": 2}', '2026-09-14 16:48:04', '2026-09-14 16:48:04');
INSERT INTO `ai_message` VALUES (65, 8, 'TOOL', '工具调用成功', 'findAvailableAppointments', '{\"name\": \"findAvailableAppointments\", \"success\": true, \"durationMs\": 8, \"resultCount\": 1}', '2026-09-14 16:48:04', '2026-09-14 16:48:04');
INSERT INTO `ai_message` VALUES (66, 8, 'ASSISTANT', '已为您确认预约信息：宠物为元宝（petId=3），医生为陈医生（外科），时段为2026-09-15 14:00-14:30（slotId=10），就诊原因：腿受伤。请确认后提交，当前仅为预约草稿，等待用户确认，尚未创建预约。', NULL, '{\"petId\": 3, \"reason\": \"腿受伤\", \"slotId\": 10, \"summary\": \"元宝（橘猫）预约陈医生（外科）2026-09-15 14:00-14:30，就诊原因：腿受伤\"}', '2026-09-14 16:48:04', '2026-09-14 16:48:04');

-- ----------------------------
-- Table structure for feedback
-- ----------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `category` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `contact` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `reply` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `replied_by` bigint NULL DEFAULT NULL,
  `replied_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_feedback_user`(`user_id` ASC) USING BTREE,
  INDEX `fk_feedback_replier`(`replied_by` ASC) USING BTREE,
  INDEX `idx_feedback_status`(`status` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_feedback_replier` FOREIGN KEY (`replied_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of feedback
-- ----------------------------
INSERT INTO `feedback` VALUES (1, 3, 'SERVICE', '候诊区建议', '希望候诊区增加猫犬分区。', '13800000003', 'REPLIED', '感谢建议，我们正在调整候诊区布局。', 2, '2026-08-20 16:00:00', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `feedback` VALUES (2, 4, 'SYSTEM', '预约提醒', '建议增加预约前一天提醒。', 'li@example.com', 'PENDING', NULL, NULL, NULL, '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for medical_record
-- ----------------------------
DROP TABLE IF EXISTS `medical_record`;
CREATE TABLE `medical_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `visit_id` bigint NOT NULL,
  `pet_id` bigint NOT NULL,
  `vet_id` bigint NOT NULL,
  `symptoms` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `diagnosis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `treatment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `prescription` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_record_visit`(`visit_id` ASC) USING BTREE,
  INDEX `fk_record_vet`(`vet_id` ASC) USING BTREE,
  INDEX `idx_record_pet`(`pet_id` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_record_pet` FOREIGN KEY (`pet_id`) REFERENCES `pet` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_record_vet` FOREIGN KEY (`vet_id`) REFERENCES `vet` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_record_visit` FOREIGN KEY (`visit_id`) REFERENCES `visit` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medical_record
-- ----------------------------
INSERT INTO `medical_record` VALUES (1, 1, 1, 1, '食欲下降、精神一般', '轻度胃肠不适', '调整饮食并观察三天', '益生菌每日一次', '如持续呕吐立即复诊', '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `publisher_id` bigint NULL DEFAULT NULL,
  `published_at` datetime NULL DEFAULT NULL,
  `expires_at` datetime NULL DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_notice_publisher`(`publisher_id` ASC) USING BTREE,
  INDEX `idx_notice_active`(`status` ASC, `expires_at` ASC, `sort_order` ASC) USING BTREE,
  CONSTRAINT `fk_notice_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (1, '夏季防暑提醒', '高温天气请避免中午遛狗，并确保宠物有充足饮水。', 'PUBLISHED', 1, '2026-08-15 09:00:00', '2026-09-30 23:59:59', 10, '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `notice` VALUES (2, '周末门诊安排', '本周六正常接诊，周日下午仅开放急诊。', 'PUBLISHED', 2, '2026-08-20 12:00:00', '2026-08-24 23:59:59', 20, '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `module` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `operation` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `http_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `request_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `response_status` int NULL DEFAULT NULL,
  `duration_ms` bigint NULL DEFAULT NULL,
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_log_user_time`(`user_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 45 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of operation_log
-- ----------------------------
INSERT INTO `operation_log` VALUES (1, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 3527, '127.0.0.1', NULL, '2026-09-11 11:53:10', '2026-09-11 11:53:10');
INSERT INTO `operation_log` VALUES (2, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 1216, '127.0.0.1', 'ai返回了空内容，请重试', '2026-09-11 11:53:29', '2026-09-11 11:53:29');
INSERT INTO `operation_log` VALUES (3, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 3889, '127.0.0.1', NULL, '2026-09-11 11:53:58', '2026-09-11 11:53:58');
INSERT INTO `operation_log` VALUES (4, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 4, '127.0.0.1', 'Error creating bean with name \'chatClientBuilder\' defined in class path resource [org/springframework/ai/model/chat/client/autoconfigure/ChatClientAutoConfiguration.class]: Unsatisfied dependency expressed through method \'chatClientBuilder\' parameter 1: No qualifying bean of type \'org.springframework.ai.chat.model.ChatModel\' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}', '2026-09-11 11:54:53', '2026-09-11 11:54:53');
INSERT INTO `operation_log` VALUES (5, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 5, '127.0.0.1', 'AI服务未启用，请配置DEEPSEEK_API_KEY，并设置AI_CHAT_MODEL=openai', '2026-09-11 11:56:59', '2026-09-11 11:56:59');
INSERT INTO `operation_log` VALUES (6, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 3525, '127.0.0.1', NULL, '2026-09-11 11:58:51', '2026-09-11 11:58:51');
INSERT INTO `operation_log` VALUES (7, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 4165, '127.0.0.1', NULL, '2026-09-11 15:02:08', '2026-09-11 15:02:08');
INSERT INTO `operation_log` VALUES (8, 3, 'owner_a', 'AiController', 'delete', 'DELETE', '/api/ai/conversations/3', 200, 7, '127.0.0.1', NULL, '2026-09-11 15:02:08', '2026-09-11 15:02:08');
INSERT INTO `operation_log` VALUES (9, 1, 'admin', 'ScheduleController', 'create', 'POST', '/api/slots', 200, 9, '127.0.0.1', NULL, '2026-09-11 15:07:20', '2026-09-11 15:07:20');
INSERT INTO `operation_log` VALUES (10, 1, 'admin', 'PetController', 'create', 'POST', '/api/pets', 200, 7, '127.0.0.1', NULL, '2026-09-11 15:09:10', '2026-09-11 15:09:10');
INSERT INTO `operation_log` VALUES (11, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 14205, '127.0.0.1', 'ai返回了空内容，请重试', '2026-09-14 16:05:49', '2026-09-14 16:05:49');
INSERT INTO `operation_log` VALUES (12, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 3048, '127.0.0.1', 'ai连接超时或暂时不可用，请稍后重试', '2026-09-14 16:06:07', '2026-09-14 16:06:07');
INSERT INTO `operation_log` VALUES (13, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 15767, '127.0.0.1', 'ai返回了空内容，请重试', '2026-09-14 16:06:26', '2026-09-14 16:06:26');
INSERT INTO `operation_log` VALUES (14, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 9954, '127.0.0.1', 'ai返回了空内容，请重试', '2026-09-14 16:07:19', '2026-09-14 16:07:19');
INSERT INTO `operation_log` VALUES (15, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 1817, '127.0.0.1', NULL, '2026-09-14 16:07:41', '2026-09-14 16:07:41');
INSERT INTO `operation_log` VALUES (16, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 10078, '127.0.0.1', NULL, '2026-09-14 16:08:14', '2026-09-14 16:08:14');
INSERT INTO `operation_log` VALUES (17, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 9860, '127.0.0.1', 'ai返回了空内容，请重试', '2026-09-14 16:13:46', '2026-09-14 16:13:46');
INSERT INTO `operation_log` VALUES (18, 4, 'owner_b', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 6157, '0:0:0:0:0:0:0:1', NULL, '2026-09-14 16:22:43', '2026-09-14 16:22:43');
INSERT INTO `operation_log` VALUES (19, 4, 'owner_b', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 12503, '0:0:0:0:0:0:0:1', NULL, '2026-09-14 16:22:55', '2026-09-14 16:22:55');
INSERT INTO `operation_log` VALUES (20, 4, 'owner_b', 'AiController', 'delete', 'DELETE', '/api/ai/conversations/4', 200, 6, '0:0:0:0:0:0:0:1', NULL, '2026-09-14 16:22:55', '2026-09-14 16:22:55');
INSERT INTO `operation_log` VALUES (21, 4, 'owner_b', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 19232, '0:0:0:0:0:0:0:1', NULL, '2026-09-14 16:23:28', '2026-09-14 16:23:28');
INSERT INTO `operation_log` VALUES (22, 4, 'owner_b', 'AiController', 'delete', 'DELETE', '/api/ai/conversations/5', 200, 3, '0:0:0:0:0:0:0:1', NULL, '2026-09-14 16:23:28', '2026-09-14 16:23:28');
INSERT INTO `operation_log` VALUES (23, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 20364, '127.0.0.1', 'AI连续返回空内容或错误格式，请稍后重试', '2026-09-14 16:25:43', '2026-09-14 16:25:43');
INSERT INTO `operation_log` VALUES (24, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 15773, '127.0.0.1', 'AI连续返回空内容或错误格式，请稍后重试', '2026-09-14 16:26:33', '2026-09-14 16:26:33');
INSERT INTO `operation_log` VALUES (25, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 12432, '0:0:0:0:0:0:0:1', NULL, '2026-09-14 16:29:02', '2026-09-14 16:29:02');
INSERT INTO `operation_log` VALUES (26, 3, 'owner_a', 'AiController', 'delete', 'DELETE', '/api/ai/conversations/6', 200, 6, '0:0:0:0:0:0:0:1', NULL, '2026-09-14 16:29:02', '2026-09-14 16:29:02');
INSERT INTO `operation_log` VALUES (27, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 500, 17536, '127.0.0.1', 'AI连续返回空内容或错误格式，请稍后重试', '2026-09-14 16:30:06', '2026-09-14 16:30:06');
INSERT INTO `operation_log` VALUES (28, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 13953, '127.0.0.1', NULL, '2026-09-14 16:34:59', '2026-09-14 16:34:59');
INSERT INTO `operation_log` VALUES (29, 3, 'owner_a', 'AiController', 'delete', 'DELETE', '/api/ai/conversations/7', 200, 6, '127.0.0.1', NULL, '2026-09-14 16:34:59', '2026-09-14 16:34:59');
INSERT INTO `operation_log` VALUES (30, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 15325, '127.0.0.1', NULL, '2026-09-14 16:36:59', '2026-09-14 16:36:59');
INSERT INTO `operation_log` VALUES (31, 3, 'owner_a', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 8634, '127.0.0.1', NULL, '2026-09-14 16:38:25', '2026-09-14 16:38:25');
INSERT INTO `operation_log` VALUES (32, 3, 'owner_a', 'VisitController', 'create', 'POST', '/api/visits', 200, 13, '127.0.0.1', NULL, '2026-09-14 16:43:37', '2026-09-14 16:43:37');
INSERT INTO `operation_log` VALUES (33, 3, 'owner_a', 'VisitController', 'cancel', 'POST', '/api/visits/3/cancel', 200, 6, '127.0.0.1', NULL, '2026-09-14 16:43:37', '2026-09-14 16:43:37');
INSERT INTO `operation_log` VALUES (34, 3, 'owner_a', 'VisitController', 'create', 'POST', '/api/visits', 200, 27, '127.0.0.1', NULL, '2026-09-14 16:45:34', '2026-09-14 16:45:34');
INSERT INTO `operation_log` VALUES (35, 3, 'owner_a', 'VisitController', 'cancel', 'POST', '/api/visits/4/cancel', 200, 14, '127.0.0.1', NULL, '2026-09-14 16:45:55', '2026-09-14 16:45:55');
INSERT INTO `operation_log` VALUES (36, 4, 'owner_b', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 6134, '127.0.0.1', NULL, '2026-09-14 16:47:05', '2026-09-14 16:47:05');
INSERT INTO `operation_log` VALUES (37, 4, 'owner_b', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 19363, '127.0.0.1', NULL, '2026-09-14 16:47:37', '2026-09-14 16:47:37');
INSERT INTO `operation_log` VALUES (38, 4, 'owner_b', 'AiController', 'chat', 'POST', '/api/ai/chat', 200, 10914, '127.0.0.1', NULL, '2026-09-14 16:48:04', '2026-09-14 16:48:04');
INSERT INTO `operation_log` VALUES (39, 4, 'owner_b', 'VisitController', 'create', 'POST', '/api/visits', 500, 505, '127.0.0.1', 'requestId 或预约时段已被使用', '2026-09-14 16:48:35', '2026-09-14 16:48:35');
INSERT INTO `operation_log` VALUES (40, 4, 'owner_b', 'VisitController', 'create', 'POST', '/api/visits', 500, 17, '127.0.0.1', 'requestId 或预约时段已被使用', '2026-09-14 16:48:40', '2026-09-14 16:48:40');
INSERT INTO `operation_log` VALUES (41, 4, 'owner_b', 'VisitController', 'create', 'POST', '/api/visits', 500, 18, '127.0.0.1', 'requestId 或预约时段已被使用', '2026-09-14 16:49:15', '2026-09-14 16:49:15');
INSERT INTO `operation_log` VALUES (42, 4, 'owner_b', 'VisitController', 'create', 'POST', '/api/visits', 200, 14, '127.0.0.1', NULL, '2026-09-14 16:52:15', '2026-09-14 16:52:15');
INSERT INTO `operation_log` VALUES (43, 4, 'owner_b', 'VisitController', 'cancel', 'POST', '/api/visits/8/cancel', 200, 5, '127.0.0.1', NULL, '2026-09-14 16:52:15', '2026-09-14 16:52:15');
INSERT INTO `operation_log` VALUES (44, 4, 'owner_b', 'VisitController', 'create', 'POST', '/api/visits', 200, 26, '127.0.0.1', NULL, '2026-09-14 16:53:20', '2026-09-14 16:53:20');

-- ----------------------------
-- Table structure for owner
-- ----------------------------
DROP TABLE IF EXISTS `owner`;
CREATE TABLE `owner`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_owner_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_owner_name_phone`(`name` ASC, `phone` ASC) USING BTREE,
  CONSTRAINT `fk_owner_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of owner
-- ----------------------------
INSERT INTO `owner` VALUES (1, 3, '张女士', '13800000003', 'zhang@example.com', '上海市浦东新区云台路 18 号', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `owner` VALUES (2, 4, '李先生', '13800000004', 'li@example.com', '上海市徐汇区桂林路 66 号', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for pet
-- ----------------------------
DROP TABLE IF EXISTS `pet`;
CREATE TABLE `pet`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_id` bigint NOT NULL,
  `type_id` bigint NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `gender` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `breed` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `birth_date` date NULL DEFAULT NULL,
  `color` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `microchip_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `allergies` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `photo_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pet_microchip`(`microchip_no` ASC) USING BTREE,
  INDEX `fk_pet_type`(`type_id` ASC) USING BTREE,
  INDEX `idx_pet_owner_status`(`owner_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `fk_pet_owner` FOREIGN KEY (`owner_id`) REFERENCES `owner` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_pet_type` FOREIGN KEY (`type_id`) REFERENCES `pet_type` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pet
-- ----------------------------
INSERT INTO `pet` VALUES (1, 1, 1, '糯米', 'FEMALE', '英短', '2022-04-12', '蓝白', 'MC20220001', '青霉素', NULL, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `pet` VALUES (2, 1, 2, '可乐', 'MALE', '柯基', '2021-09-03', '黄白', 'MC20210002', NULL, NULL, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `pet` VALUES (3, 2, 1, '元宝', 'MALE', '橘猫', '2023-01-20', '橘色', 'MC20230003', NULL, NULL, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `pet` VALUES (4, 2, 3, '团团', 'FEMALE', '垂耳兔', '2024-05-08', '白色', NULL, NULL, NULL, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `pet` VALUES (5, 1, 1, 'sx', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', '2026-09-11 15:09:09', '2026-09-11 15:09:09');

-- ----------------------------
-- Table structure for pet_type
-- ----------------------------
DROP TABLE IF EXISTS `pet_type`;
CREATE TABLE `pet_type`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pet_type_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pet_type
-- ----------------------------
INSERT INTO `pet_type` VALUES (1, '猫', '家猫及常见猫科宠物', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `pet_type` VALUES (2, '犬', '各类家养犬', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `pet_type` VALUES (3, '兔', '家兔和垂耳兔', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `pet_type` VALUES (4, '其他', '鸟类、仓鼠等小型宠物', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for specialty
-- ----------------------------
DROP TABLE IF EXISTS `specialty`;
CREATE TABLE `specialty`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_specialty_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of specialty
-- ----------------------------
INSERT INTO `specialty` VALUES (1, '全科', '常规检查与常见疾病', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `specialty` VALUES (2, '外科', '手术及创伤处理', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `specialty` VALUES (3, '皮肤科', '皮肤和毛发疾病', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `specialty` VALUES (4, '口腔科', '牙齿与口腔疾病', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `specialty` VALUES (5, '影像科', 'X 光与超声诊断', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NULL DEFAULT NULL,
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `path` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `icon` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_permission_code`(`code` ASC) USING BTREE,
  INDEX `fk_permission_parent`(`parent_id` ASC) USING BTREE,
  CONSTRAINT `fk_permission_parent` FOREIGN KEY (`parent_id`) REFERENCES `sys_permission` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_permission
-- ----------------------------
INSERT INTO `sys_permission` VALUES (1, NULL, 'dashboard:read', '数据看板', 'MENU', '/dashboard', 'DataAnalysis', 10, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (2, NULL, 'owner:manage', '主人管理', 'MENU', '/owners', 'User', 20, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (3, NULL, 'pet:manage', '宠物管理', 'MENU', '/pets', 'MostlyCloudy', 30, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (4, NULL, 'vet:manage', '兽医管理', 'MENU', '/vets', 'Avatar', 40, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (5, NULL, 'schedule:manage', '排班管理', 'MENU', '/schedules', 'Calendar', 50, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (6, NULL, 'visit:manage', '预约管理', 'MENU', '/visits', 'Tickets', 60, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (7, NULL, 'medical:manage', '病历管理', 'MENU', '/medical-records', 'Document', 70, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (8, NULL, 'vaccination:manage', '疫苗管理', 'MENU', '/vaccinations', 'FirstAidKit', 80, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (9, NULL, 'notice:manage', '公告管理', 'MENU', '/notices', 'Bell', 90, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (10, NULL, 'feedback:manage', '反馈管理', 'MENU', '/feedback', 'ChatLineSquare', 100, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (11, NULL, 'system:manage', '系统管理', 'MENU', '/system/users', 'Setting', 110, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (12, NULL, 'ai:chat', 'AI 助手', 'MENU', '/ai', 'MagicStick', 120, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (13, 3, 'pet:create', '新增宠物', 'BUTTON', NULL, NULL, 1, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (14, 3, 'pet:update', '修改宠物', 'BUTTON', NULL, NULL, 2, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (15, 3, 'pet:delete', '停用宠物', 'BUTTON', NULL, NULL, 3, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (16, 6, 'visit:create', '创建预约', 'BUTTON', NULL, NULL, 1, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_permission` VALUES (17, 6, 'visit:cancel', '取消预约', 'BUTTON', NULL, NULL, 2, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_role_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'ADMIN', '管理员', '全部管理权限', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_role` VALUES (2, 'STAFF', '员工', '诊所日常业务', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_role` VALUES (3, 'OWNER', '宠物主人', '个人宠物和预约', 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`  (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`) USING BTREE,
  INDEX `fk_rp_permission`(`permission_id` ASC) USING BTREE,
  CONSTRAINT `fk_rp_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_permission
-- ----------------------------
INSERT INTO `sys_role_permission` VALUES (1, 1);
INSERT INTO `sys_role_permission` VALUES (2, 1);
INSERT INTO `sys_role_permission` VALUES (1, 2);
INSERT INTO `sys_role_permission` VALUES (2, 2);
INSERT INTO `sys_role_permission` VALUES (1, 3);
INSERT INTO `sys_role_permission` VALUES (2, 3);
INSERT INTO `sys_role_permission` VALUES (3, 3);
INSERT INTO `sys_role_permission` VALUES (1, 4);
INSERT INTO `sys_role_permission` VALUES (2, 4);
INSERT INTO `sys_role_permission` VALUES (1, 5);
INSERT INTO `sys_role_permission` VALUES (2, 5);
INSERT INTO `sys_role_permission` VALUES (3, 5);
INSERT INTO `sys_role_permission` VALUES (1, 6);
INSERT INTO `sys_role_permission` VALUES (2, 6);
INSERT INTO `sys_role_permission` VALUES (3, 6);
INSERT INTO `sys_role_permission` VALUES (1, 7);
INSERT INTO `sys_role_permission` VALUES (2, 7);
INSERT INTO `sys_role_permission` VALUES (1, 8);
INSERT INTO `sys_role_permission` VALUES (2, 8);
INSERT INTO `sys_role_permission` VALUES (3, 8);
INSERT INTO `sys_role_permission` VALUES (1, 9);
INSERT INTO `sys_role_permission` VALUES (2, 9);
INSERT INTO `sys_role_permission` VALUES (3, 9);
INSERT INTO `sys_role_permission` VALUES (1, 10);
INSERT INTO `sys_role_permission` VALUES (2, 10);
INSERT INTO `sys_role_permission` VALUES (3, 10);
INSERT INTO `sys_role_permission` VALUES (1, 11);
INSERT INTO `sys_role_permission` VALUES (1, 12);
INSERT INTO `sys_role_permission` VALUES (3, 12);
INSERT INTO `sys_role_permission` VALUES (1, 13);
INSERT INTO `sys_role_permission` VALUES (3, 13);
INSERT INTO `sys_role_permission` VALUES (1, 14);
INSERT INTO `sys_role_permission` VALUES (3, 14);
INSERT INTO `sys_role_permission` VALUES (1, 15);
INSERT INTO `sys_role_permission` VALUES (3, 15);
INSERT INTO `sys_role_permission` VALUES (1, 16);
INSERT INTO `sys_role_permission` VALUES (3, 16);
INSERT INTO `sys_role_permission` VALUES (1, 17);
INSERT INTO `sys_role_permission` VALUES (3, 17);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `account_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `token_version` int NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_username`(`username` ASC) USING BTREE,
  INDEX `idx_sys_user_status_type`(`status` ASC, `account_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$QRNmcpMAXwRReRFmDkCvy.y/ObWLAHTCMPE6UB7nZZcRT9vADtj7e', '系统管理员', '13800000001', 'admin@petclinic.local', 'ADMIN', 'ACTIVE', 1, '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_user` VALUES (2, 'staff', '$2a$10$QRNmcpMAXwRReRFmDkCvy.y/ObWLAHTCMPE6UB7nZZcRT9vADtj7e', '前台员工', '13800000002', 'staff@petclinic.local', 'STAFF', 'ACTIVE', 1, '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_user` VALUES (3, 'owner_a', '$2a$10$QRNmcpMAXwRReRFmDkCvy.y/ObWLAHTCMPE6UB7nZZcRT9vADtj7e', '张女士', '13800000003', 'zhang@example.com', 'OWNER', 'ACTIVE', 1, '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `sys_user` VALUES (4, 'owner_b', '$2a$10$QRNmcpMAXwRReRFmDkCvy.y/ObWLAHTCMPE6UB7nZZcRT9vADtj7e', '李先生', '13800000004', 'li@example.com', 'OWNER', 'ACTIVE', 1, '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE,
  INDEX `fk_ur_role`(`role_id` ASC) USING BTREE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);
INSERT INTO `sys_user_role` VALUES (2, 2);
INSERT INTO `sys_user_role` VALUES (3, 3);
INSERT INTO `sys_user_role` VALUES (4, 3);

-- ----------------------------
-- Table structure for vaccination_record
-- ----------------------------
DROP TABLE IF EXISTS `vaccination_record`;
CREATE TABLE `vaccination_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pet_id` bigint NOT NULL,
  `vaccine_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `batch_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `vaccinated_date` date NOT NULL,
  `next_due_date` date NULL DEFAULT NULL,
  `veterinarian` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VALID',
  `notes` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_vaccine_due`(`next_due_date` ASC, `status` ASC) USING BTREE,
  INDEX `idx_vaccine_pet`(`pet_id` ASC, `vaccinated_date` ASC) USING BTREE,
  CONSTRAINT `fk_vaccine_pet` FOREIGN KEY (`pet_id`) REFERENCES `pet` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vaccination_record
-- ----------------------------
INSERT INTO `vaccination_record` VALUES (1, 1, '猫三联', 'CAT-2026-018', '2026-03-10', '2027-03-10', '王医生', 'VALID', '年度加强针', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vaccination_record` VALUES (2, 2, '犬六联', 'DOG-2026-022', '2026-02-15', '2027-02-15', '陈医生', 'VALID', '无不良反应', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vaccination_record` VALUES (3, 3, '狂犬疫苗', 'RAB-2025-901', '2025-07-01', '2026-07-01', '王医生', 'OVERDUE', '需要尽快补种', '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for vet
-- ----------------------------
DROP TABLE IF EXISTS `vet`;
CREATE TABLE `vet`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `license_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `biography` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_vet_license`(`license_no` ASC) USING BTREE,
  INDEX `idx_vet_name_status`(`name` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vet
-- ----------------------------
INSERT INTO `vet` VALUES (1, '王医生', '13900000001', 'wang@petclinic.local', 'VET-SH-1001', '十年小动物全科经验', NULL, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vet` VALUES (2, '陈医生', '13900000002', 'chen@petclinic.local', 'VET-SH-1002', '擅长软组织外科与骨科', NULL, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vet` VALUES (3, '周医生', '13900000003', 'zhou@petclinic.local', 'VET-SH-1003', '专注宠物皮肤病与过敏', NULL, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vet` VALUES (4, '赵医生', '13900000004', 'zhao@petclinic.local', 'VET-SH-1004', '口腔及影像联合诊疗', NULL, 'ACTIVE', '2026-08-21 11:18:24', '2026-08-21 11:18:24');

-- ----------------------------
-- Table structure for vet_schedule_slot
-- ----------------------------
DROP TABLE IF EXISTS `vet_schedule_slot`;
CREATE TABLE `vet_schedule_slot`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `vet_id` bigint NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE',
  `note` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_vet_slot`(`vet_id` ASC, `start_time` ASC) USING BTREE,
  INDEX `idx_slot_available`(`vet_id` ASC, `status` ASC, `start_time` ASC) USING BTREE,
  CONSTRAINT `fk_slot_vet` FOREIGN KEY (`vet_id`) REFERENCES `vet` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vet_schedule_slot
-- ----------------------------
INSERT INTO `vet_schedule_slot` VALUES (1, 1, '2026-08-22 09:00:00', '2026-08-22 09:30:00', 'BOOKED', '上午门诊', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vet_schedule_slot` VALUES (2, 1, '2026-08-22 09:30:00', '2026-08-22 10:00:00', 'AVAILABLE', '上午门诊', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vet_schedule_slot` VALUES (3, 2, '2026-08-22 14:00:00', '2026-08-22 14:30:00', 'BOOKED', '外科门诊', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vet_schedule_slot` VALUES (4, 2, '2026-08-22 14:30:00', '2026-08-22 15:00:00', 'AVAILABLE', '外科门诊', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vet_schedule_slot` VALUES (5, 3, '2026-08-23 10:00:00', '2026-08-23 10:30:00', 'AVAILABLE', '皮肤科', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vet_schedule_slot` VALUES (6, 4, '2026-08-23 15:00:00', '2026-08-23 15:30:00', 'AVAILABLE', '口腔科', '2026-08-21 11:18:24', '2026-08-21 11:18:24');
INSERT INTO `vet_schedule_slot` VALUES (7, 1, '2026-09-12 12:00:00', '2026-09-12 15:00:00', 'AVAILABLE', NULL, '2026-09-11 15:07:20', '2026-09-11 15:07:20');
INSERT INTO `vet_schedule_slot` VALUES (8, 1, '2026-09-15 09:00:00', '2026-09-15 09:30:00', 'AVAILABLE', '全科上午门诊', '2026-09-14 16:00:39', '2026-09-14 16:00:39');
INSERT INTO `vet_schedule_slot` VALUES (9, 1, '2026-09-15 09:30:00', '2026-09-15 10:00:00', 'AVAILABLE', '全科上午门诊', '2026-09-14 16:00:39', '2026-09-14 16:00:39');
INSERT INTO `vet_schedule_slot` VALUES (10, 2, '2026-09-15 14:00:00', '2026-09-15 14:30:00', 'BOOKED', '外科下午门诊', '2026-09-14 16:00:39', '2026-09-14 16:53:19');
INSERT INTO `vet_schedule_slot` VALUES (11, 2, '2026-09-15 14:30:00', '2026-09-15 15:00:00', 'AVAILABLE', '外科下午门诊', '2026-09-14 16:00:39', '2026-09-14 16:43:36');
INSERT INTO `vet_schedule_slot` VALUES (12, 3, '2026-09-16 10:00:00', '2026-09-16 10:30:00', 'AVAILABLE', '皮肤科门诊', '2026-09-14 16:00:39', '2026-09-14 16:00:39');
INSERT INTO `vet_schedule_slot` VALUES (13, 3, '2026-09-16 10:30:00', '2026-09-16 11:00:00', 'AVAILABLE', '皮肤科门诊', '2026-09-14 16:00:39', '2026-09-14 16:00:39');
INSERT INTO `vet_schedule_slot` VALUES (14, 4, '2026-09-16 15:00:00', '2026-09-16 15:30:00', 'AVAILABLE', '口腔科门诊', '2026-09-14 16:00:39', '2026-09-14 16:00:39');
INSERT INTO `vet_schedule_slot` VALUES (15, 4, '2026-09-16 15:30:00', '2026-09-16 16:00:00', 'AVAILABLE', '影像联合门诊', '2026-09-14 16:00:39', '2026-09-14 16:00:39');
INSERT INTO `vet_schedule_slot` VALUES (16, 1, '2026-09-17 09:00:00', '2026-09-17 09:30:00', 'AVAILABLE', '全科上午门诊', '2026-09-14 16:00:39', '2026-09-14 16:00:39');
INSERT INTO `vet_schedule_slot` VALUES (17, 2, '2026-09-17 14:00:00', '2026-09-17 14:30:00', 'AVAILABLE', '外科下午门诊', '2026-09-14 16:00:39', '2026-09-14 16:00:39');
INSERT INTO `vet_schedule_slot` VALUES (18, 3, '2026-09-18 10:00:00', '2026-09-18 10:30:00', 'AVAILABLE', '皮肤科门诊', '2026-09-14 16:00:39', '2026-09-14 16:00:39');
INSERT INTO `vet_schedule_slot` VALUES (19, 4, '2026-09-18 15:00:00', '2026-09-18 15:30:00', 'AVAILABLE', '口腔科门诊', '2026-09-14 16:00:39', '2026-09-14 16:00:39');

-- ----------------------------
-- Table structure for vet_specialty
-- ----------------------------
DROP TABLE IF EXISTS `vet_specialty`;
CREATE TABLE `vet_specialty`  (
  `vet_id` bigint NOT NULL,
  `specialty_id` bigint NOT NULL,
  PRIMARY KEY (`vet_id`, `specialty_id`) USING BTREE,
  INDEX `fk_vs_specialty`(`specialty_id` ASC) USING BTREE,
  CONSTRAINT `fk_vs_specialty` FOREIGN KEY (`specialty_id`) REFERENCES `specialty` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_vs_vet` FOREIGN KEY (`vet_id`) REFERENCES `vet` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vet_specialty
-- ----------------------------
INSERT INTO `vet_specialty` VALUES (1, 1);
INSERT INTO `vet_specialty` VALUES (2, 1);
INSERT INTO `vet_specialty` VALUES (3, 1);
INSERT INTO `vet_specialty` VALUES (2, 2);
INSERT INTO `vet_specialty` VALUES (3, 3);
INSERT INTO `vet_specialty` VALUES (4, 4);
INSERT INTO `vet_specialty` VALUES (4, 5);

-- ----------------------------
-- Table structure for visit
-- ----------------------------
DROP TABLE IF EXISTS `visit`;
CREATE TABLE `visit`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pet_id` bigint NOT NULL,
  `slot_id` bigint NOT NULL,
  `vet_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SCHEDULED',
  `cancelled_at` datetime NULL DEFAULT NULL,
  `cancel_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_visit_request`(`request_id` ASC) USING BTREE,
  INDEX `fk_visit_pet`(`pet_id` ASC) USING BTREE,
  INDEX `idx_visit_owner_status`(`created_by` ASC, `status` ASC, `id` ASC) USING BTREE,
  INDEX `idx_visit_vet_status`(`vet_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_visit_slot`(`slot_id` ASC) USING BTREE,
  CONSTRAINT `fk_visit_pet` FOREIGN KEY (`pet_id`) REFERENCES `pet` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_visit_slot` FOREIGN KEY (`slot_id`) REFERENCES `vet_schedule_slot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_visit_user` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_visit_vet` FOREIGN KEY (`vet_id`) REFERENCES `vet` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of visit
-- ----------------------------
INSERT INTO `visit` VALUES (1, 1, 1, 1, 3, 'DEMO-REQ-001', '近期食欲下降，需要常规检查', 'COMPLETED', NULL, NULL, '2026-08-20 10:00:00', '2026-08-21 11:18:24');
INSERT INTO `visit` VALUES (2, 3, 3, 2, 4, 'DEMO-REQ-002', '后腿轻微跛行', 'SCHEDULED', NULL, NULL, '2026-08-21 09:30:00', '2026-08-21 11:18:24');
INSERT INTO `visit` VALUES (4, 5, 10, 2, 3, 'ai-2-5-10', '腿受伤', 'CANCELLED', '2026-09-14 16:45:55', '。。。', '2026-09-14 16:45:34', '2026-09-14 16:45:55');
INSERT INTO `visit` VALUES (9, 3, 10, 2, 4, 'ai-8-3-10', '腿受伤', 'SCHEDULED', NULL, NULL, '2026-09-14 16:53:20', '2026-09-14 16:53:20');

SET FOREIGN_KEY_CHECKS = 1;
