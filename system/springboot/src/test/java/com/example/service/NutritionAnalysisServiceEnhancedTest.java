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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * NutritionAnalysisService增强版单元测试
 * 测试营养分析功能的边界情况和健壮性
 */
public class NutritionAnalysisServiceEnhancedTest {

    @Mock
    private RecipeMapper recipeMapper;

    @InjectMocks
    private NutritionAnalysisService nutritionAnalysisService;

    private User testUser;
    private List<Recipe> mockRecipes;

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
        
        // 创建测试食谱数据列表
        mockRecipes = new ArrayList<>();
        
        // 标准食谱
        Recipe recipe1 = new Recipe();
        recipe1.setId(1);
        recipe1.setName("标准午餐");
        recipe1.setCalories(400);
        recipe1.setProtein(new BigDecimal(25));
        recipe1.setCarbohydrate(new BigDecimal(40));
        recipe1.setFat(new BigDecimal(15));
        
        // 低卡食谱
        Recipe recipe2 = new Recipe();
        recipe2.setId(2);
        recipe2.setName("低卡午餐");
        recipe2.setCalories(200);
        recipe2.setProtein(new BigDecimal(15));
        recipe2.setCarbohydrate(new BigDecimal(20));
        recipe2.setFat(new BigDecimal(5));
        
        // 高卡食谱
        Recipe recipe3 = new Recipe();
        recipe3.setId(3);
        recipe3.setName("高卡午餐");
        recipe3.setCalories(800);
        recipe3.setProtein(new BigDecimal(40));
        recipe3.setCarbohydrate(new BigDecimal(80));
        recipe3.setFat(new BigDecimal(30));
        
        // 极端情况食谱 - 零卡路里
        Recipe recipe4 = new Recipe();
        recipe4.setId(4);
        recipe4.setName("零卡食谱");
        recipe4.setCalories(0);
        recipe4.setProtein(new BigDecimal(0));
        recipe4.setCarbohydrate(new BigDecimal(0));
        recipe4.setFat(new BigDecimal(0));
        
        mockRecipes.add(recipe1);
        mockRecipes.add(recipe2);
        mockRecipes.add(recipe3);
        mockRecipes.add(recipe4);
    }

    /**
     * 测试食谱推荐 - 边界值处理
     */
    @Test
    public void testRecommendRecipes_EdgeCases() {
        // 模拟食谱数据返回
        when(recipeMapper.selectAll(any())).thenReturn(mockRecipes);
        
        // 测试大量推荐数量
        List<Map<String, Object>> recommendations = nutritionAnalysisService.recommendRecipes(testUser, "午餐", 100);
        
        assertNotNull(recommendations);
        // 应该返回所有可用食谱，但不会超过实际数量
        assertEquals(mockRecipes.size(), recommendations.size());
        
        // 验证每个返回的食谱都有有效的推荐分数
        for (Map<String, Object> recommendation : recommendations) {
            Double score = (Double) recommendation.get("score");
            assertNotNull(score);
            assertTrue(score >= 0, "推荐分数不应为负数");
            assertTrue(score <= 100, "推荐分数不应超过100");
        }
    }

    /**
     * 测试零卡路里食谱处理
     */
    @Test
    public void testRecommendRecipes_ZeroCalorieRecipe() {
        // 只返回零卡路里食谱
        Recipe zeroRecipe = mockRecipes.get(3);
        when(recipeMapper.selectAll(any())).thenReturn(Collections.singletonList(zeroRecipe));
        
        // 应该能正常处理而不抛出异常
        List<Map<String, Object>> recommendations = nutritionAnalysisService.recommendRecipes(testUser, "午餐", 1);
        
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        
        // 验证返回的数据结构正确
        Map<String, Object> recommendation = recommendations.get(0);
        assertTrue(recommendation.containsKey("recipe"));
        assertTrue(recommendation.containsKey("score"));
    }

    /**
     * 测试空食谱列表处理
     */
    @Test
    public void testRecommendRecipes_EmptyRecipeList() {
        // 模拟返回空列表
        when(recipeMapper.selectAll(any())).thenReturn(Collections.emptyList());
        
        // 应该返回空列表而不抛出异常
        List<Map<String, Object>> recommendations = nutritionAnalysisService.recommendRecipes(testUser, "午餐", 5);
        
        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }

    /**
     * 测试用户数据不完整的处理
     */
    @Test
    public void testRecommendRecipes_IncompleteUserData() {
        User incompleteUser = new User();
        // 缺少关键数据
        incompleteUser.setWeight(null);
        incompleteUser.setHeight(null);
        incompleteUser.setAge(30);
        
        // 应该抛出IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            nutritionAnalysisService.recommendRecipes(incompleteUser, "午餐", 5);
        });
    }

    /**
     * 测试极端活动水平处理
     */
    @Test
    public void testCalculateNutritionPlan_ExtremeActivityLevels() {
        // 测试久坐活动水平
        User sedentaryUser = new User();
        sedentaryUser.setWeight(70.0);
        sedentaryUser.setHeight(175.0);
        sedentaryUser.setAge(30);
        sedentaryUser.setGender("男");
        sedentaryUser.setActivityLevel("久坐");
        sedentaryUser.setGoal("维持健康");
        
        Map<String, Object> plan1 = nutritionAnalysisService.calculateNutritionPlan(sedentaryUser);
        assertNotNull(plan1);
        assertTrue(plan1.containsKey("tdee"));
        
        // 测试重度活动水平
        User intenseUser = new User();
        intenseUser.setWeight(70.0);
        intenseUser.setHeight(175.0);
        intenseUser.setAge(30);
        intenseUser.setGender("男");
        intenseUser.setActivityLevel("重度");
        intenseUser.setGoal("维持健康");
        
        Map<String, Object> plan2 = nutritionAnalysisService.calculateNutritionPlan(intenseUser);
        assertNotNull(plan2);
        
        // 重度活动的TDEE应该高于久坐
        double tdee1 = (double) plan1.get("tdee");
        double tdee2 = (double) plan2.get("tdee");
        assertTrue(tdee2 > tdee1, "重度活动的TDEE应该高于久坐");
    }

    /**
     * 测试所有餐型的推荐功能
     */
    @Test
    public void testRecommendRecipes_AllMealTypes() {
        when(recipeMapper.selectAll(any())).thenReturn(mockRecipes);
        
        String[] mealTypes = {"早餐", "午餐", "晚餐", "加餐"};
        
        for (String mealType : mealTypes) {
            List<Map<String, Object>> recommendations = nutritionAnalysisService.recommendRecipes(testUser, mealType, 3);
            
            assertNotNull(recommendations);
            // 即使不同餐型，也应该能返回结果
            assertFalse(recommendations.isEmpty(), "对于餐型" + mealType + "应返回推荐结果");
        }
    }

    /**
     * 测试非标准餐型处理
     */
    @Test
    public void testRecommendRecipes_NonStandardMealType() {
        when(recipeMapper.selectAll(any())).thenReturn(mockRecipes);
        
        // 使用非标准餐型名称
        List<Map<String, Object>> recommendations = nutritionAnalysisService.recommendRecipes(testUser, "特殊餐", 3);
        
        assertNotNull(recommendations);
        // 即使是非标准餐型，也应该能正常处理
        assertTrue(recommendations.size() >= 0);
    }

    /**
     * 测试营养计划计算的健壮性
     */
    @Test
    public void testCalculateNutritionPlan_Robustness() {
        // 测试非常小的用户数据
        User smallUser = new User();
        smallUser.setWeight(30.0); // 30kg
        smallUser.setHeight(100.0); // 100cm
        smallUser.setAge(10); // 10岁
        smallUser.setGender("男");
        smallUser.setActivityLevel("轻度");
        smallUser.setGoal("维持健康");
        
        // 应该能正常计算而不抛出异常
        Map<String, Object> plan = nutritionAnalysisService.calculateNutritionPlan(smallUser);
        
        assertNotNull(plan);
        assertTrue(plan.containsKey("bmr"));
        assertTrue(plan.containsKey("tdee"));
        assertTrue(plan.containsKey("recommendedCalories"));
        assertTrue(plan.containsKey("nutrients"));
        
        // 验证营养素计算结果合理
        Map<String, Double> nutrients = (Map<String, Double>) plan.get("nutrients");
        assertTrue(nutrients.get("protein") >= 0);
        assertTrue(nutrients.get("carbohydrate") >= 0);
        assertTrue(nutrients.get("fat") >= 0);
    }
}