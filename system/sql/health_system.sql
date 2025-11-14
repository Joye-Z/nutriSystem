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

 Date: 14/11/2025 21:20:25
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
INSERT INTO `admin` VALUES (1, 'admin', 'e661c0a237922d3e3354234a00548ab6', '管理员', 'http://localhost:9090/files/download/1763122015350-微信图片_20230707210551.jpg', 'ADMIN');

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
INSERT INTO `recipe` VALUES (1, '番茄炒蛋', '简单美味的家常菜', '番茄2个,鸡蛋3个,葱花适量,盐适量,油适量', '番茄切块，鸡蛋打散备用\n热锅凉油，倒入鸡蛋液炒熟盛出\n锅中留底油，放入番茄块翻炒\n加入炒好的鸡蛋，调入盐和葱花', 15, 180, 12.50, 8.20, 10.30, '家常菜', '简单', 'http://localhost:9090/files/download/1763121300039-番茄炒蛋.png', '2024-11-27 10:00:00', '2025-11-14 19:55:03');
INSERT INTO `recipe` VALUES (2, '红烧肉', '经典家常菜', '五花肉500g,冰糖适量,生姜3片,料酒2勺,生抽2勺,老抽1勺', '五花肉切块焯水\n锅中放冰糖炒糖色\n放入肉块翻炒上色\n加入调料和适量水，小火慢炖1小时', 90, 450, 25.80, 12.30, 35.60, '家常菜', '中等', 'http://localhost:9090/files/download/1763121019322-红烧肉.png', '2024-11-27 10:05:00', '2025-11-14 19:50:28');
INSERT INTO `recipe` VALUES (3, '红烧肉plus', '超美味！！！', '水\n油\n肉', '洗\n蒸\n焖', 60, 300, 123.00, 200.00, 300.00, '家常菜', '中等', 'http://localhost:9090/files/download/1763107914989-红烧肉.png', '2025-11-14 16:14:05', '2025-11-14 19:31:44');

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
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '普通用户信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'abc', 'fc296d8443ddb3c9efae89e97d049096', 'abc', 'http://localhost:9090/files/download/1763126238733-微信图片_20230722083917.jpg', 'USER', '1874634733', 'abc@163.com');
INSERT INTO `user` VALUES (6, 'aaa', 'c95968929283564bef0ba6810dfb10aa', '张三', 'http://localhost:9090/files/download/1763126225306-007Ryzjmgy1h5fnnwmcaqj32qr1jke81.jpg', 'USER', '13463536741', 'aaa@163.com');
INSERT INTO `user` VALUES (7, 'bbb', 'a89ef1974afe603cf2ae93bde02164d5', '李四', 'http://localhost:9090/files/download/1763126212877-微信图片_20230707210533.jpg', 'USER', '1385346437', 'bbb@163.com');

SET FOREIGN_KEY_CHECKS = 1;
