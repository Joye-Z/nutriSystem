package com.example.service;

import com.example.entity.Recipe;
import com.example.mapper.RecipeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 食谱业务处理
 */
@Service
public class RecipeService {

    @Resource
    private RecipeMapper recipeMapper;

    /**
     * 新增
     */
    public void add(Recipe recipe) {
        recipeMapper.insert(recipe);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        recipeMapper.deleteById(id);
    }

    /**
     * 修改
     */
    public void updateById(Recipe recipe) {
        recipeMapper.updateById(recipe);
    }

    /**
     * 根据ID查询
     */
    public Recipe selectById(Integer id) {
        return recipeMapper.selectById(id);
    }

    /**
     * 查询所有（支持按食材、热量、烹饪时间筛选）
     */
    public List<Recipe> selectAll(Recipe recipe) {
        return recipeMapper.selectAll(recipe);
    }

    /**
     * 分页查询
     */
    public PageInfo<Recipe> selectPage(Recipe recipe, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Recipe> list = recipeMapper.selectAll(recipe);
        return PageInfo.of(list);
    }

    /**
     * 根据分类查询
     */
    public List<Recipe> selectByCategory(String category) {
        return recipeMapper.selectByCategory(category);
    }

    /**
     * 根据食材搜索
     */
    public List<Recipe> searchByIngredients(String keyword) {
        Recipe recipe = new Recipe();
        recipe.setIngredients(keyword);
        return recipeMapper.selectAll(recipe);
    }
}