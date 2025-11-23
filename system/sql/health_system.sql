/*
 Navicat Premium Data Transfer

 Source Server         : db_user
 Source Server Type    : MySQL
 Source Server Version : 80030
 Source Host           : localhost:3306
 Source Schema         : health_system

 Target Server Type    : MySQL
Target Server Version : 80030
 File Encoding         : 65001

 Date: 23/11/2025 21:48:44
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '账号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (1, 'admin', 'e661c0a237922d3e3354234a00548ab6', '管理员', 'https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-23/d63b9b8115d040bc9d6c8f7762d3fbe6.jpg', 'ADMIN');

-- ----------------------------
-- Table structure for meal_plan
-- ----------------------------
DROP TABLE IF EXISTS `meal_plan`;
CREATE TABLE `meal_plan`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `plan_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '计划名称',
  `plan_date` date NOT NULL COMMENT '计划日期',
  `meal_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '餐次类型(早餐/午餐/晚餐/加餐)',
  `recipe_id` int NULL DEFAULT NULL COMMENT '食谱ID',
  `custom_meal` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '自定义餐食内容',
  `calories` int NULL DEFAULT NULL COMMENT '热量(卡路里)',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_date`(`user_id` ASC, `plan_date` ASC) USING BTREE,
  INDEX `idx_recipe`(`recipe_id` ASC) USING BTREE,
  CONSTRAINT `fk_meal_plan_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_meal_plan_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '膳食计划表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of meal_plan
-- ----------------------------
INSERT INTO `meal_plan` VALUES (1, 1, '20午餐计划', '2025-11-20', '午餐', 1, '', 260, '配大白米饭', '2025-11-19 22:14:35', '2025-11-23 21:43:26');
INSERT INTO `meal_plan` VALUES (2, 8, '22吃饱饱计划', '2025-11-22', '晚餐', NULL, '', 300, '', '2025-11-19 22:17:28', '2025-11-19 22:17:28');
INSERT INTO `meal_plan` VALUES (3, 1, '26计划', '2025-11-26', '早餐', NULL, '牛奶+面包', 200, '', '2025-11-23 21:45:18', '2025-11-23 21:45:18');
INSERT INTO `meal_plan` VALUES (4, 8, '28好好吃饭计划', '2025-11-28', '午餐', 1, '', 400, '配大白米饭', '2025-11-23 21:47:43', '2025-11-23 21:47:43');

-- ----------------------------
-- Table structure for recipe
-- ----------------------------
DROP TABLE IF EXISTS `recipe`;
CREATE TABLE `recipe`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '食谱名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '食谱描述',
  `ingredients` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '食材清单',
  `steps` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '烹饪步骤',
  `cooking_time` int NULL DEFAULT NULL COMMENT '烹饪时间(分钟)',
  `calories` int NULL DEFAULT NULL COMMENT '热量(卡路里)',
  `protein` decimal(10, 2) NULL DEFAULT NULL COMMENT '蛋白质(g)',
  `carbohydrate` decimal(10, 2) NULL DEFAULT NULL COMMENT '碳水化合物(g)',
  `fat` decimal(10, 2) NULL DEFAULT NULL COMMENT '脂肪(g)',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类',
  `difficulty` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '难度',
  `image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '食谱图片',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '食谱信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of recipe
-- ----------------------------
INSERT INTO `recipe` VALUES (1, '红烧肉', '经典家常菜，色香味俱全', NULL, NULL, 60, 450, NULL, NULL, NULL, '家常菜', '中等', 'https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-23/b052803ff849456a9c5b9f4d225cca35.png', '2024-11-27 10:00:00', '2025-11-23 21:33:45');
INSERT INTO `recipe` VALUES (4, '番茄鸡蛋', '传统的菜肴，别致的做法，独特的口感~~~', '番茄（大） 2个\n鸡蛋 3个\n葱花 20克\n姜末 10克\n蒜末 20克\n盐糖 少许\n番茄沙司 少许', '葱姜蒜切末，葱花和蒜末可以切得粗一些\n番茄切中等大小的块儿\n鸡蛋里面加一小撮盐，炒成蛋花\n锅里倒适量油，大火爆香葱姜蒜末\n倒入番茄，大火翻炒几下\n加入鸡蛋，1勺番茄沙司， 大火翻炒1分钟\n加入一小勺盐，少许糖（喜欢甜的多放点），翻炒均匀，撒把葱花起锅即可', 20, 300, NULL, NULL, NULL, '家常菜', '简单', 'https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-23/7b72ef1adfd64e38b3425120f7f7fd23.png', '2025-11-23 21:35:58', '2025-11-23 21:35:58');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '账号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `height` decimal(10, 2) NULL DEFAULT NULL COMMENT '身高(cm)',
  `weight` decimal(10, 2) NULL DEFAULT NULL COMMENT '体重(kg)',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `gender` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '性别',
  `activity_level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '活动水平',
  `goal` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '健康目标',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '普通用户信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'abc', 'fc296d8443ddb3c9efae89e97d049096', 'abc', 'https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-23/ef5acf97c9ac4d64a72609e5ce9d56b5.jpg', 'USER', '1874634733', 'abc@163.com', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user` VALUES (6, 'aaa', 'c95968929283564bef0ba6810dfb10aa', '张三', 'https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-23/f363d26456fe4231be728e3e507aab6b.jpg', 'USER', '13463536741', 'aaa@163.com', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user` VALUES (7, 'bbb', 'a89ef1974afe603cf2ae93bde02164d5', '李四', 'https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-23/08d55733823d479281371bb104d5b6c7.jpg', 'USER', '1385346437', 'bbb@163.com', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user` VALUES (8, 'user', '5a07eacedbd164a9cbfa69c6b6d7e266', 'user', 'https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-23/dad79a45e10f4f6793a4de4fc8334755.jpg', 'USER', NULL, NULL, 160.00, 55.00, 22, '女', '轻度', '维持健康');

SET FOREIGN_KEY_CHECKS = 1;
