package com.example.mapper;

import com.example.entity.Recipe;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 食谱数据访问层
 */
public interface RecipeMapper {

    /**
     * 查询所有食谱（支持条件查询）
     */
    List<Recipe> selectAll(Recipe recipe);

    /**
     * 根据ID查询
     */
    @Select("select * from recipe where id = #{id}")
    Recipe selectById(Integer id);

    /**
     * 新增
     */
    void insert(Recipe recipe);

    /**
     * 修改
     */
    void updateById(Recipe recipe);

    /**
     * 删除
     */
    @Select("delete from recipe where id = #{id}")
    void deleteById(Integer id);

    /**
     * 根据分类查询
     */
    @Select("select * from recipe where category = #{category} order by create_time desc")
    List<Recipe> selectByCategory(String category);
}