package com.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 膳食计划实体类
 * 用于存储用户的每日膳食计划信息，包括计划名称、日期、餐次类型、关联食谱等
 */
public class MealPlan {
    
    /** ID */
    private Integer id;
    
    /** 用户ID */
    private Integer userId;
    
    /** 计划名称 */
    private String planName;
    
    /** 计划日期 */
    private LocalDate planDate;
    
    /** 餐次类型(早餐/午餐/晚餐/加餐) */
    private String mealType;
    
    /** 食谱ID */
    private Integer recipeId;
    
    /** 自定义餐食内容 */
    private String customMeal;
    
    /** 热量(卡路里) */
    private Integer calories;
    
    /** 备注 */
    private String notes;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    // 关联的食谱信息
    private Recipe recipe;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getCustomMeal() {
        return customMeal;
    }

    public void setCustomMeal(String customMeal) {
        this.customMeal = customMeal;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}