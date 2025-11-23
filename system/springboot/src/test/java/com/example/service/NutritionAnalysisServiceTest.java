package com.example.service;

import com.example.entity.User;
import com.example.entity.Recipe;
import com.example.mapper.RecipeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * NutritionAnalysisService单元测试
 * 验证个性化营养分析功能的正确性
 */
public class NutritionAnalysisServiceTest {

    @Mock
    private RecipeMapper recipeMapper;

    @InjectMocks
    private NutritionAnalysisService nutritionAnalysisService;

    private User testUser;
    private Recipe testRecipe1;
    private Recipe testRecipe2;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建测试用户数据
        testUser = new User();
        testUser.setWeight(70.0); // 70kg
        testUser.setHeight(175.0); // 175cm
        testUser.setAge(30); // 30岁
        testUser.setGender("男"); // 男性
        testUser.setActivityLevel("中度"); // 中度活动
        testUser.setGoal("维持健康"); // 维持健康
        
        // 创建测试食谱数据
        testRecipe1 = new Recipe();
        testRecipe1.setId(1);
        testRecipe1.setName("健康午餐");
        testRecipe1.setCalories(400);
        testRecipe1.setProtein(new BigDecimal(25));
        testRecipe1.setCarbohydrate(new BigDecimal(40));
        testRecipe1.setFat(new BigDecimal(15));
        
        testRecipe2 = new Recipe();
        testRecipe2.setId(2);
        testRecipe2.setName("高蛋白晚餐");
        testRecipe2.setCalories(500);
        testRecipe2.setProtein(new BigDecimal(35));
        testRecipe2.setCarbohydrate(new BigDecimal(30));
        testRecipe2.setFat(new BigDecimal(20));
    }

    /**
     * 测试BMR计算功能 - 男性
     */
    @Test
    public void testCalculateBMR_Male() {
        // 预期结果使用Mifflin-St Jeor公式：10*70 + 6.25*175 - 5*30 + 5 = 1637.5
        double bmr = nutritionAnalysisService.calculateBMR(testUser);
        assertEquals(1637.5, bmr, 0.1);
    }

    /**
     * 测试BMR计算功能 - 女性
     */
    @Test
    public void testCalculateBMR_Female() {
        testUser.setGender("女");
        // 预期结果使用Mifflin-St Jeor公式：10*70 + 6.25*175 - 5*30 - 161 = 1471.5
        double bmr = nutritionAnalysisService.calculateBMR(testUser);
        assertEquals(1471.5, bmr, 0.1);
    }

    /**
     * 测试TDEE计算功能
     */
    @Test
    public void testCalculateTDEE() {
        double bmr = 1637.5;
        // 中度活动水平系数为1.55
        double tdee = nutritionAnalysisService.calculateTDEE(bmr, "中度");
        assertEquals(1637.5 * 1.55, tdee, 0.1);
    }

    /**
     * 测试推荐卡路里计算 - 不同健康目标
     */
    @Test
    public void testCalculateRecommendedCalories() {
        double tdee = 2538.125; // BMR 1637.5 * 1.55
        
        // 维持健康目标
        testUser.setGoal("维持健康");
        double maintainCalories = nutritionAnalysisService.calculateRecommendedCalories(tdee, "维持健康");
        assertEquals(tdee, maintainCalories, 0.1);
        
        // 减重目标
        testUser.setGoal("减重");
        double weightLossCalories = nutritionAnalysisService.calculateRecommendedCalories(tdee, "减重");
        assertEquals(tdee - 400, weightLossCalories, 0.1);
        
        // 增肌目标
        testUser.setGoal("增肌");
        double muscleGainCalories = nutritionAnalysisService.calculateRecommendedCalories(tdee, "增肌");
        assertEquals(tdee + 250, muscleGainCalories, 0.1);
    }

    /**
     * 测试营养素计算功能
     */
    @Test
    public void testCalculateNutrientRequirements() {
        double recommendedCalories = 2538.125;
        
        // 维持健康目标：蛋白质20%、碳水50%、脂肪30%
        Map<String, Double> nutrients = nutritionAnalysisService.calculateNutrientRequirements(recommendedCalories, "维持健康");
        
        // 蛋白质：2538.125 * 0.2 / 4 ≈ 126.9g
        assertEquals(126.9, nutrients.get("protein"), 0.1);
        // 碳水化合物：2538.125 * 0.5 / 4 ≈ 317.3g
        assertEquals(317.3, nutrients.get("carbohydrate"), 0.1);
        // 脂肪：2538.125 * 0.3 / 9 ≈ 84.6g
        assertEquals(84.6, nutrients.get("fat"), 0.1);
    }

    /**
     * 测试完整的营养计划计算
     */
    @Test
    public void testCalculateNutritionPlan() {
        Map<String, Object> nutritionPlan = nutritionAnalysisService.calculateNutritionPlan(testUser);
        
        assertNotNull(nutritionPlan);
        assertTrue(nutritionPlan.containsKey("bmr"));
        assertTrue(nutritionPlan.containsKey("tdee"));
        assertTrue(nutritionPlan.containsKey("recommendedCalories"));
        assertTrue(nutritionPlan.containsKey("nutrients"));
        
        // 验证BMR值
        assertEquals(1637L, nutritionPlan.get("bmr"));
    }

    /**
     * 测试食谱推荐功能
     */
    @Test
    public void testRecommendRecipes() {
        // 模拟食谱数据返回
        when(recipeMapper.selectAll(any())).thenReturn(Arrays.asList(testRecipe1, testRecipe2));
        
        // 执行推荐
        List<Map<String, Object>> recommendations = nutritionAnalysisService.recommendRecipes(testUser, "午餐", 2);
        
        assertNotNull(recommendations);
        assertEquals(2, recommendations.size());
        
        // 验证返回的数据结构
        for (Map<String, Object> recommendation : recommendations) {
            assertTrue(recommendation.containsKey("recipe"));
            assertTrue(recommendation.containsKey("score"));
            assertNotNull(recommendation.get("score"));
        }
        
        // 验证方法调用
        verify(recipeMapper).selectAll(any());
    }

    /**
     * 测试参数验证 - 不完整的用户数据
     */
    @Test
    public void testCalculateBMR_InvalidUserData() {
        User invalidUser = new User();
        
        // 缺少必要字段
        assertThrows(IllegalArgumentException.class, () -> {
            nutritionAnalysisService.calculateBMR(invalidUser);
        });
    }

    /**
     * 测试食谱匹配得分计算
     */
    @Test
    public void testCalculateRecipeScore() {
        // 通过反射调用私有方法进行测试
        try {
            java.lang.reflect.Method method = NutritionAnalysisService.class.getDeclaredMethod(
                "calculateRecipeScore", Recipe.class, double.class, double.class, double.class, double.class);
            method.setAccessible(true);
            
            // 目标：400卡路里，25g蛋白质，40g碳水，15g脂肪
            double score = (double) method.invoke(nutritionAnalysisService, testRecipe1, 400, 25, 40, 15);
            
            // 完全匹配的食谱应该得到接近100的分数
            assertTrue(score > 95, "完全匹配的食谱得分应该很高");
            
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }
}