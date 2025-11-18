package com.example.controller;

import com.example.common.Result;
import com.example.entity.MealPlan;
import com.example.service.MealPlanService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mealPlan")
public class MealPlanController {

    @Resource
    private MealPlanService mealPlanService;

    /**
     * 新增膳食计划
     */
    @PostMapping("/add")
    public Result add(@RequestBody MealPlan mealPlan) {
        mealPlanService.add(mealPlan);
        return Result.success();
    }

    /**
     * 删除膳食计划
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        mealPlanService.deleteById(id);
        return Result.success();
    }

    /**
     * 修改膳食计划
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody MealPlan mealPlan) {
        mealPlanService.updateById(mealPlan);
        return Result.success();
    }

    /**
     * 根据ID查询膳食计划
     */
    @GetMapping("/select/{id}")
    public Result selectById(@PathVariable Integer id) {
        MealPlan mealPlan = mealPlanService.selectById(id);
        return Result.success(mealPlan);
    }

    /**
     * 根据用户ID查询膳食计划
     */
    @GetMapping("/selectByUser")
    public Result selectByUserId(@RequestParam Integer userId,
                                @RequestParam(required = false) String planName,
                                @RequestParam(required = false) String planDate,
                                @RequestParam(required = false) String mealType,
                                @RequestParam(required = false) Integer pageNum,
                                @RequestParam(required = false) Integer pageSize) {
        LocalDate date = null;
        if (planDate != null && !planDate.trim().isEmpty()) {
            date = LocalDate.parse(planDate);
        }
        
        // 如果提供了分页参数，则使用分页查询
        if (pageNum != null && pageSize != null) {
            Map<String, Object> result = mealPlanService.selectByUserIdWithPage(userId, planName, date, mealType, pageNum, pageSize);
            return Result.success(result);
        } else {
            // 没有分页参数，返回所有数据
            List<MealPlan> mealPlans = mealPlanService.selectByUserId(userId, planName, date, mealType);
            return Result.success(mealPlans);
        }
    }

    /**
     * 查询日期范围内的膳食计划
     */
    @GetMapping("/selectByDateRange")
    public Result selectByDateRange(@RequestParam Integer userId,
                                   @RequestParam String startDate,
                                   @RequestParam String endDate) {
        if (startDate == null || startDate.trim().isEmpty() || endDate == null || endDate.trim().isEmpty()) {
            return Result.error("开始日期和结束日期不能为空");
        }
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<MealPlan> mealPlans = mealPlanService.selectByDateRange(userId, start, end);
        return Result.success(mealPlans);
    }

    /**
     * 检查某日期是否有膳食计划
     */
    @GetMapping("/hasPlan")
    public Result hasPlanForDate(@RequestParam Integer userId, @RequestParam String date) {
        if (date == null || date.trim().isEmpty()) {
            return Result.error("日期不能为空");
        }
        LocalDate localDate = LocalDate.parse(date);
        boolean hasPlan = mealPlanService.hasPlanForDate(userId, localDate);
        return Result.success(hasPlan);
    }
}