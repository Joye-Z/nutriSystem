package com.example.mapper;

import com.example.entity.MealPlan;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MealPlanMapper {

    @Insert("INSERT INTO meal_plan (user_id, plan_name, plan_date, meal_type, recipe_id, custom_meal, calories, notes) " +
            "VALUES (#{userId}, #{planName}, #{planDate}, #{mealType}, #{recipeId}, #{customMeal}, #{calories}, #{notes})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MealPlan mealPlan);

    @Delete("DELETE FROM meal_plan WHERE id = #{id}")
    int deleteById(Integer id);

    @Update("UPDATE meal_plan SET plan_name = #{planName}, plan_date = #{planDate}, meal_type = #{mealType}, " +
            "recipe_id = #{recipeId}, custom_meal = #{customMeal}, calories = #{calories}, notes = #{notes}, " +
            "update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateById(MealPlan mealPlan);

    @Select("SELECT * FROM meal_plan WHERE id = #{id}")
    MealPlan selectById(Integer id);

    @Select("<script>" +
            "SELECT mp.*, r.name as recipe_name, r.image as recipe_image " +
            "FROM meal_plan mp " +
            "LEFT JOIN recipe r ON mp.recipe_id = r.id " +
            "WHERE mp.user_id = #{userId} " +
            "<if test='planName != null and planName != \"\"'>AND mp.plan_name LIKE CONCAT('%', #{planName}, '%')</if>" +
            "<if test='planDate != null'>AND mp.plan_date = #{planDate}</if>" +
            "<if test='mealType != null'>AND mp.meal_type = #{mealType}</if>" +
            "ORDER BY mp.plan_date DESC, mp.meal_type" +
            "</script>")
    List<MealPlan> selectByUserId(@Param("userId") Integer userId, 
                                @Param("planName") String planName,
                                @Param("planDate") LocalDate planDate,
                                @Param("mealType") String mealType);

    @Select("<script>" +
            "SELECT mp.*, r.name as recipe_name, r.image as recipe_image " +
            "FROM meal_plan mp " +
            "LEFT JOIN recipe r ON mp.recipe_id = r.id " +
            "WHERE mp.user_id = #{userId} " +
            "<if test='planName != null and planName != \"\"'>AND mp.plan_name LIKE CONCAT('%', #{planName}, '%')</if>" +
            "<if test='planDate != null'>AND mp.plan_date = #{planDate}</if>" +
            "<if test='mealType != null'>AND mp.meal_type = #{mealType}</if>" +
            "ORDER BY mp.plan_date DESC, mp.meal_type " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<MealPlan> selectByUserIdWithPage(@Param("userId") Integer userId, 
                                        @Param("planName") String planName,
                                        @Param("planDate") LocalDate planDate,
                                        @Param("mealType") String mealType,
                                        @Param("offset") Integer offset,
                                        @Param("pageSize") Integer pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM meal_plan mp " +
            "WHERE mp.user_id = #{userId} " +
            "<if test='planName != null and planName != \"\"'>AND mp.plan_name LIKE CONCAT('%', #{planName}, '%')</if>" +
            "<if test='planDate != null'>AND mp.plan_date = #{planDate}</if>" +
            "<if test='mealType != null'>AND mp.meal_type = #{mealType}</if>" +
            "</script>")
    int countByUserId(@Param("userId") Integer userId, 
                    @Param("planName") String planName,
                    @Param("planDate") LocalDate planDate,
                    @Param("mealType") String mealType);

    @Select("SELECT * FROM meal_plan WHERE user_id = #{userId} AND plan_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY plan_date, meal_type")
    List<MealPlan> selectByDateRange(@Param("userId") Integer userId, 
                                    @Param("startDate") LocalDate startDate, 
                                    @Param("endDate") LocalDate endDate);

    @Select("SELECT COUNT(*) FROM meal_plan WHERE user_id = #{userId} AND plan_date = #{date}")
    int countByUserAndDate(@Param("userId") Integer userId, @Param("date") LocalDate date);
}