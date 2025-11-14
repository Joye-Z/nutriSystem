package com.example.controller;

import com.example.common.Result;
import com.example.entity.Recipe;
import com.example.service.RecipeService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 食谱前端操作接口
 */
@RestController
@RequestMapping("/recipe")
public class RecipeController {

    @Resource
    private RecipeService recipeService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Recipe recipe) {
        recipeService.add(recipe);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        recipeService.deleteById(id);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Recipe recipe) {
        recipeService.updateById(recipe);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Recipe recipe = recipeService.selectById(id);
        return Result.success(recipe);
    }

    /**
     * 查询所有（支持筛选）
     */
    @GetMapping("/selectAll")
    public Result selectAll(Recipe recipe) {
        List<Recipe> list = recipeService.selectAll(recipe);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Recipe recipe,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Recipe> page = recipeService.selectPage(recipe, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 根据分类查询
     */
    @GetMapping("/selectByCategory/{category}")
    public Result selectByCategory(@PathVariable String category) {
        List<Recipe> list = recipeService.selectByCategory(category);
        return Result.success(list);
    }

}