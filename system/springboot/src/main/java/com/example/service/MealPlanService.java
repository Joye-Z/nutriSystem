package com.example.service;

import com.example.entity.MealPlan;
import com.example.mapper.MealPlanMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MealPlanService {

    @Resource
    private MealPlanMapper mealPlanMapper;

    public void add(MealPlan mealPlan) {
        mealPlanMapper.insert(mealPlan);
    }

    public void deleteById(Integer id) {
        mealPlanMapper.deleteById(id);
    }

    public void updateById(MealPlan mealPlan) {
        mealPlanMapper.updateById(mealPlan);
    }

    public MealPlan selectById(Integer id) {
        return mealPlanMapper.selectById(id);
    }

    public List<MealPlan> selectByUserId(Integer userId, String planName, LocalDate planDate, String mealType) {
        return mealPlanMapper.selectByUserId(userId, planName, planDate, mealType);
    }

    public Map<String, Object> selectByUserIdWithPage(Integer userId, String planName, LocalDate planDate, String mealType, Integer pageNum, Integer pageSize) {
        // 计算偏移量
        Integer offset = (pageNum - 1) * pageSize;
        
        // 查询数据列表
        List<MealPlan> data = mealPlanMapper.selectByUserIdWithPage(userId, planName, planDate, mealType, offset, pageSize);
        
        // 查询总数
        Integer total = mealPlanMapper.countByUserId(userId, planName, planDate, mealType);
        
        // 返回分页结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", data);
        result.put("total", total);
        
        return result;
    }

    public List<MealPlan> selectByDateRange(Integer userId, LocalDate startDate, LocalDate endDate) {
        return mealPlanMapper.selectByDateRange(userId, startDate, endDate);
    }

    public boolean hasPlanForDate(Integer userId, LocalDate date) {
        return mealPlanMapper.countByUserAndDate(userId, date) > 0;
    }
}