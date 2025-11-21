package com.example.controller;

import com.example.common.Result;
import com.example.entity.User;
import com.example.service.NutritionAnalysisService;
import com.example.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 个性化营养分析接口
 * 提供基于用户健康数据的营养计算和食谱推荐服务
 */
@RestController
@RequestMapping("/nutritionAnalysis")
public class NutritionAnalysisController {

    // 添加日志记录器
    private static final Logger logger = LoggerFactory.getLogger(NutritionAnalysisController.class);
    
    @Resource
    private NutritionAnalysisService nutritionAnalysisService;

    @Resource
    private UserService userService;

    /**
     * 计算用户营养计划
     * @param userId 用户ID
     * @return 营养计划结果（包含BMR、TDEE、营养素需求）
     */
    @GetMapping("/nutritionPlan/{userId}")
    public Result calculateNutritionPlan(@PathVariable Integer userId) {
        logger.info("获取用户营养计划请求，用户ID: {}", userId);
        try {
            User user = userService.selectById(userId);
            if (user == null) {
                logger.warn("用户不存在，用户ID: {}", userId);
                return Result.error("用户不存在");
            }
            
            Map<String, Object> nutritionPlan = nutritionAnalysisService.calculateNutritionPlan(user);
            logger.info("获取用户营养计划成功，用户ID: {}", userId);
            return Result.success(nutritionPlan);
        } catch (IllegalArgumentException e) {
            logger.warn("参数验证失败，用户ID: {}, 错误信息: {}", userId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("获取用户营养计划失败，用户ID: {}, 错误信息: {}", userId, e.getMessage(), e);
            return Result.error("营养计划计算失败，请稍后重试");
        }
    }

    /**
     * 计算营养计划（直接传入用户数据）
     * @param user 用户对象
     * @return 营养计划结果
     */
    @PostMapping("/nutritionPlan")
    public Result calculateNutritionPlan(@RequestBody User user) {
        logger.info("计算用户营养计划请求，用户ID: {}", user.getId());
        try {
            Map<String, Object> nutritionPlan = nutritionAnalysisService.calculateNutritionPlan(user);
            logger.info("计算用户营养计划成功，用户ID: {}", user.getId());
            return Result.success(nutritionPlan);
        } catch (IllegalArgumentException e) {
            logger.warn("参数验证失败，用户ID: {}, 错误信息: {}", user.getId(), e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("计算用户营养计划失败，用户ID: {}, 错误信息: {}", user.getId(), e.getMessage(), e);
            return Result.error("营养计划计算失败，请稍后重试");
        }
    }

    /**
     * 获取特定餐别的食谱推荐
     * @param userId 用户ID
     * @param mealType 餐别（早餐/午餐/晚餐/加餐）
     * @param count 推荐数量
     * @return 推荐食谱列表
     */
    @GetMapping("/recipes")
    public Result getRecipeRecommendations(
            @RequestParam Integer userId,
            @RequestParam String mealType,
            @RequestParam(defaultValue = "3") Integer count) {
        logger.info("获取食谱推荐请求，用户ID: {}, 餐别: {}, 推荐数量: {}", userId, mealType, count);
        try {
            User user = userService.selectById(userId);
            if (user == null) {
                logger.warn("用户不存在，用户ID: {}", userId);
                return Result.error("用户不存在");
            }
            
            List<Map<String, Object>> recommendations = nutritionAnalysisService.recommendRecipes(user, mealType, count);
            logger.info("获取食谱推荐成功，用户ID: {}", userId);
            return Result.success(recommendations);
        } catch (IllegalArgumentException e) {
            logger.warn("参数验证失败，用户ID: {}, 错误信息: {}", userId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("获取食谱推荐失败，用户ID: {}, 错误信息: {}", userId, e.getMessage(), e);
            return Result.error("食谱推荐失败，请稍后重试");
        }
    }

    /**
     * 获取完整的膳食计划
     * @param userId 用户ID
     * @return 包含各餐推荐的完整膳食计划
     */
    @GetMapping("/mealPlan/{userId}")
    public Result getFullMealPlan(@PathVariable Integer userId) {
        logger.info("获取膳食计划请求，用户ID: {}", userId);
        try {
            User user = userService.selectById(userId);
            if (user == null) {
                logger.warn("用户不存在，用户ID: {}", userId);
                return Result.error("用户不存在");
            }
            
            Map<String, List<Map<String, Object>>> mealPlan = nutritionAnalysisService.generateMealPlan(user);
            logger.info("获取膳食计划成功，用户ID: {}", userId);
            return Result.success(mealPlan);
        } catch (IllegalArgumentException e) {
            logger.warn("参数验证失败，用户ID: {}, 错误信息: {}", userId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("获取膳食计划失败，用户ID: {}, 错误信息: {}", userId, e.getMessage(), e);
            return Result.error("膳食计划生成失败，请稍后重试");
        }
    }

    /**
     * 更新用户健康数据
     * @param userId 用户ID
     * @param healthData 健康数据
     * @return 更新结果
     */
    @PutMapping("/updateHealthData/{userId}")
    public Result updateHealthData(
            @PathVariable Integer userId,
            @RequestBody Map<String, Object> healthData) {
        logger.info("更新用户健康数据请求，用户ID: {}", userId);
        try {
            User user = userService.selectById(userId);
            if (user == null) {
                logger.warn("用户不存在，用户ID: {}", userId);
                return Result.error("用户不存在");
            }
            
            // 更新健康数据
            if (healthData.containsKey("height")) {
                user.setHeight(Double.valueOf(healthData.get("height").toString()));
                logger.debug("更新用户身高: {}", healthData.get("height"));
            }
            if (healthData.containsKey("weight")) {
                user.setWeight(Double.valueOf(healthData.get("weight").toString()));
                logger.debug("更新用户体重: {}", healthData.get("weight"));
            }
            if (healthData.containsKey("age")) {
                user.setAge(Integer.valueOf(healthData.get("age").toString()));
                logger.debug("更新用户年龄: {}", healthData.get("age"));
            }
            if (healthData.containsKey("gender")) {
                user.setGender(healthData.get("gender").toString());
                logger.debug("更新用户性别: {}", healthData.get("gender"));
            }
            if (healthData.containsKey("activityLevel")) {
                user.setActivityLevel(healthData.get("activityLevel").toString());
                logger.debug("更新用户活动水平: {}", healthData.get("activityLevel"));
            }
            if (healthData.containsKey("goal")) {
                user.setGoal(healthData.get("goal").toString());
                logger.debug("更新用户目标: {}", healthData.get("goal"));
            }
            
            // 保存更新
            userService.updateById(user);
            logger.info("用户健康数据更新成功，用户ID: {}", userId);
            
            // 清除用户相关缓存，确保下次推荐使用最新数据
            nutritionAnalysisService.clearUserCache(user);
            
            return Result.success();
        } catch (NumberFormatException e) {
            logger.warn("健康数据格式错误，用户ID: {}, 错误信息: {}", userId, e.getMessage());
            return Result.error("健康数据格式错误，请检查输入");
        } catch (Exception e) {
            logger.error("健康数据更新失败，用户ID: {}, 错误信息: {}", userId, e.getMessage(), e);
            return Result.error("健康数据更新失败，请稍后重试");
        }
    }
}