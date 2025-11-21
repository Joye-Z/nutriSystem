package com.example.service;

import com.example.entity.User;
import com.example.entity.Recipe;
import com.example.mapper.RecipeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 个性化营养分析服务
 * 实现基于用户健康数据的营养计算和食谱推荐
 */
@Service
public class NutritionAnalysisService {

    // 添加详细日志记录器
    private static final Logger logger = LoggerFactory.getLogger(NutritionAnalysisService.class);
    
    private final RecipeMapper recipeMapper;
    private final RecipeCacheService recipeCacheService;
    
    @Autowired
    public NutritionAnalysisService(RecipeMapper recipeMapper, RecipeCacheService recipeCacheService) {
        this.recipeMapper = recipeMapper;
        this.recipeCacheService = recipeCacheService;
    }

    /**
     * 计算用户基础代谢率(BMR) - 使用Mifflin-St Jeor公式
     * @param user 用户对象
     * @return 基础代谢率（卡路里）
     */
    public double calculateBMR(User user) {
        if (user == null || user.getWeight() == null || user.getHeight() == null || user.getAge() == null || user.getGender() == null) {
            throw new IllegalArgumentException("用户健康数据不完整");
        }

        double weight = user.getWeight();
        double height = user.getHeight();
        int age = user.getAge();
        String gender = user.getGender();

        if ("男".equals(gender) || "male".equalsIgnoreCase(gender)) {
            // 男性: BMR = 10 * 体重(kg) + 6.25 * 身高(cm) - 5 * 年龄 + 5
            return 10 * weight + 6.25 * height - 5 * age + 5;
        } else if ("女".equals(gender) || "female".equalsIgnoreCase(gender)) {
            // 女性: BMR = 10 * 体重(kg) + 6.25 * 身高(cm) - 5 * 年龄 - 161
            return 10 * weight + 6.25 * height - 5 * age - 161;
        } else {
            throw new IllegalArgumentException("性别数据无效");
        }
    }

    /**
     * 根据活动水平计算总能量消耗(TDEE)
     * @param bmr 基础代谢率
     * @param activityLevel 活动水平
     * @return 总能量消耗（卡路里）
     */
    public double calculateTDEE(double bmr, String activityLevel) {
        double activityFactor = getActivityFactor(activityLevel);
        return bmr * activityFactor;
    }

    /**
     * 根据健康目标调整推荐的卡路里摄入量
     * @param tdee 总能量消耗
     * @param goal 健康目标
     * @return 推荐卡路里摄入量
     */
    public double calculateRecommendedCalories(double tdee, String goal) {
        if (goal == null) {
            throw new IllegalArgumentException("健康目标不能为空");
        }

        switch (goal) {
            case "减重":
                // 减重: 减少300-500卡路里
                return tdee - 400;
            case "增肌":
                // 增肌: 增加200-300卡路里
                return tdee + 250;
            case "维持健康":
            default:
                // 维持体重: TDEE保持不变
                return tdee;
        }
    }

    /**
     * 计算每日所需营养素
     * @param recommendedCalories 推荐卡路里摄入量
     * @param goal 健康目标
     * @return 营养素需求映射（蛋白质、碳水化合物、脂肪，单位g）
     */
    public Map<String, Double> calculateNutrientRequirements(double recommendedCalories, String goal) {
        Map<String, Double> nutrients = new HashMap<>();
        
        double proteinRatio, carbRatio, fatRatio;
        
        // 根据健康目标调整营养素比例
        if (goal == null) {
            throw new IllegalArgumentException("健康目标不能为空");
        }
        
        switch (goal) {
            case "增肌":
                // 增肌: 高蛋白(25%)、中碳水(45%)、适中脂肪(30%)
                proteinRatio = 0.25;
                carbRatio = 0.45;
                fatRatio = 0.30;
                break;
            case "减重":
                // 减重: 高蛋白(30%)、低碳水(30%)、中脂肪(40%)
                proteinRatio = 0.30;
                carbRatio = 0.30;
                fatRatio = 0.40;
                break;
            case "维持健康":
            default:
                // 维持: 平衡比例 - 蛋白质(20%)、碳水(50%)、脂肪(30%)
                proteinRatio = 0.20;
                carbRatio = 0.50;
                fatRatio = 0.30;
                break;
        }
        
        // 每克蛋白质和碳水化合物提供4卡路里，每克脂肪提供9卡路里
        double protein = (recommendedCalories * proteinRatio) / 4;
        double carbs = (recommendedCalories * carbRatio) / 4;
        double fat = (recommendedCalories * fatRatio) / 9;
        
        // 四舍五入到小数点后2位
        nutrients.put("protein", Math.round(protein * 100.0) / 100.0);
        nutrients.put("carbohydrate", Math.round(carbs * 100.0) / 100.0);
        nutrients.put("fat", Math.round(fat * 100.0) / 100.0);
        
        return nutrients;
    }

    /**
     * 获取活动水平对应的活动系数
     * @param activityLevel 活动水平
     * @return 活动系数
     */
    private double getActivityFactor(String activityLevel) {
        if (activityLevel == null) {
            return 1.2; // 默认久坐
        }
        
        switch (activityLevel) {
            case "久坐":
                return 1.2; // 很少或没有运动
            case "轻度":
                return 1.375; // 每周轻度运动1-3次
            case "中度":
                return 1.55; // 每周中度运动3-5次
            case "重度":
                return 1.725; // 每周高强度运动6-7次
            default:
                return 1.2; // 默认久坐
        }
    }

    /**
     * 根据用户数据计算完整的营养需求
     * @param user 用户对象
     * @return 营养需求结果
     */
    public Map<String, Object> calculateNutritionPlan(User user) {
        logger.info("开始计算用户营养计划，用户ID: {}", user.getId());
        
        // 参数验证
        validateUserData(user);
        logger.debug("用户数据验证通过，开始计算BMR和TDEE");
        
        try {
            // 计算BMR (基础代谢率)
            double bmr = calculateBMR(user);
            logger.debug("计算得到BMR: {}", bmr);
            
            // 计算TDEE (总能量消耗)
            double tdee = calculateTDEE(bmr, user.getActivityLevel());
            logger.debug("计算得到TDEE: {}", tdee);
            
            // 根据健康目标调整推荐卡路里
            double recommendedCalories = calculateRecommendedCalories(tdee, user.getGoal());
            logger.debug("根据目标'{}'调整后的推荐卡路里: {}", user.getGoal(), recommendedCalories);
            
            // 计算宏量营养素需求
            Map<String, Double> nutrients = calculateNutrientRequirements(recommendedCalories, user.getGoal());
            logger.debug("计算得到营养素需求: {}", nutrients);
            
            // 构建返回结果，确保数值类型正确
            Map<String, Object> nutritionPlan = new HashMap<>();
            // 不使用Math.round，保持double类型以避免前端类型转换问题
            nutritionPlan.put("bmr", bmr);
            nutritionPlan.put("tdee", tdee);
            nutritionPlan.put("recommendedCalories", recommendedCalories);
            nutritionPlan.put("nutrients", nutrients);
            
            logger.info("用户营养计划计算成功，ID: {}", user.getId());
            return nutritionPlan;
        } catch (Exception e) {
            logger.error("计算营养计划时发生错误，用户ID: {}, 错误信息: {}", user.getId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 验证用户数据完整性
     * @param user 用户对象
     * @throws IllegalArgumentException 当用户数据不完整时抛出
     */
    private void validateUserData(User user) {
        if (user == null) {
            logger.error("用户对象为空");
            throw new IllegalArgumentException("用户信息不能为空");
        }
        
        List<String> errors = new ArrayList<>();
        
        // 只验证核心必填字段（身高、体重、年龄）
        if (user.getWeight() == null || user.getWeight() <= 0) {
            errors.add("体重");
        }
        if (user.getHeight() == null || user.getHeight() <= 0) {
            errors.add("身高");
        }
        if (user.getAge() == null || user.getAge() <= 0) {
            errors.add("年龄");
        }
        
        // 为非核心字段设置默认值，而不是要求必填
        if (user.getGender() == null || user.getGender().isEmpty()) {
            user.setGender("男"); // 默认男性
            logger.debug("用户性别为空，设置默认值：男");
        }
        if (user.getActivityLevel() == null || user.getActivityLevel().isEmpty()) {
            user.setActivityLevel("中度"); // 默认中度活动水平
            logger.debug("用户活动水平为空，设置默认值：中度");
        }
        if (user.getGoal() == null || user.getGoal().isEmpty()) {
            user.setGoal("维持健康"); // 默认维持健康
            logger.debug("用户健康目标为空，设置默认值：维持健康");
        }
        
        if (!errors.isEmpty()) {
            String missingFields = String.join("、", errors);
            String errorMsg = "用户健康数据不完整，请补充以下信息：" + missingFields;
            logger.warn("用户数据验证失败，缺少核心字段: {}", missingFields);
            throw new IllegalArgumentException(errorMsg);
        }
    }
    
    /**
     * 为用户推荐食谱
     * @param user 用户对象
     * @param mealType 餐别（早餐/午餐/晚餐/加餐）
     * @param count 推荐数量
     * @return 推荐的食谱列表（带置信度评分）
     */
    /**
     * 为用户推荐食谱
     * 优化版：增加食谱多样性保证，提升推荐质量，添加缓存机制
     * @param user 用户对象
     * @param mealType 餐别（早餐/午餐/晚餐/加餐）
     * @param count 推荐数量
     * @return 推荐的食谱列表（带置信度评分）
     */
    public List<Map<String, Object>> recommendRecipes(User user, String mealType, int count) {
        // 尝试从缓存获取推荐结果
            Integer userId = user.getId();
            String cacheKey = recipeCacheService.generateCacheKey(userId, mealType, count);
            List<Map<String, Object>> cachedResult = recipeCacheService.getFromCache(cacheKey);
            if (cachedResult != null) {
                logger.debug("从缓存获取推荐结果成功: 用户ID={}, 餐别={}, 数量={}", userId, mealType, count);
                // 返回缓存副本，避免直接修改缓存内容
                return new ArrayList<>(cachedResult);
            }
        // 检查用户健康数据完整性
        if (user == null || user.getHeight() == null || user.getWeight() == null || user.getAge() == null) {
            logger.error("用户健康数据不完整，无法推荐食谱");
            throw new IllegalArgumentException("用户健康数据不完整，请补充体重、身高、年龄信息");
        }
        
        try {
            // 计算用户营养需求 - 优化：添加性能监控
            long startTime = System.currentTimeMillis();
            Map<String, Object> nutritionPlan = calculateNutritionPlan(user);
            logger.debug("营养需求计算耗时: {}ms", System.currentTimeMillis() - startTime);
            
            double recommendedCalories = 2000.0; // 默认值
            Map<String, Double> nutrients = new HashMap<>();
            
            // 安全的类型转换，允许默认值
            Object caloriesObj = nutritionPlan.get("recommendedCalories");
            if (caloriesObj instanceof Number) {
                recommendedCalories = ((Number) caloriesObj).doubleValue();
                // 确保推荐卡路里为合理值
                if (recommendedCalories <= 0) {
                    recommendedCalories = 2000.0;
                    logger.warn("推荐卡路里计算结果异常，使用默认值: {}", recommendedCalories);
                }
            } else {
                logger.warn("营养计划中的推荐卡路里数据缺失或格式不正确，使用默认值: {}", recommendedCalories);
            }
            
            // 获取营养素数据，使用更宽松的处理
            Object nutrientsObj = nutritionPlan.get("nutrients");
            if (nutrientsObj instanceof Map<?, ?> nutrientsMap) {
                extractNutrients(nutrients, nutrientsMap);
            } else {
                logger.warn("营养计划中的营养素数据缺失，使用默认值");
                setDefaultNutrients(nutrients);
            }
            
            // 根据餐别和用户目标智能分配卡路里比例
            double mealCalories = calculateMealCalories(recommendedCalories, mealType, user.getGoal());
            
            // 计算每餐营养素需求，使用安全的除法
            double mealRatio = Math.max(0.01, mealCalories / recommendedCalories); // 避免除零
            double mealProtein = nutrients.getOrDefault("protein", 50.0) * mealRatio;
            double mealCarbs = nutrients.getOrDefault("carbohydrate", 250.0) * mealRatio;
            double mealFat = nutrients.getOrDefault("fat", 65.0) * mealRatio;
            
            logger.debug("为用户计算的餐食需求: 卡路里={}, 蛋白质={}, 碳水={}, 脂肪={}", 
                    mealCalories, mealProtein, mealCarbs, mealFat);
            
            // 获取所有食谱 - 优化：可以根据餐别进行预筛选
            startTime = System.currentTimeMillis();
            List<Recipe> allRecipes = getRecipesByMealType(mealType);
            logger.debug("获取食谱数据耗时: {}ms", System.currentTimeMillis() - startTime);
            
            if (allRecipes == null || allRecipes.isEmpty()) {
                logger.warn("未找到任何食谱数据");
                return new ArrayList<>();
            }
            
            // 计算每个食谱的得分 - 优化：并行处理提升性能
            startTime = System.currentTimeMillis();
            List<Map<String, Object>> scoredRecipes = calculateRecipeScoresParallel(allRecipes, mealCalories, 
                    mealProtein, mealCarbs, mealFat);
            logger.debug("食谱评分计算耗时: {}ms", System.currentTimeMillis() - startTime);
            
            // 按得分降序排序
            startTime = System.currentTimeMillis();
            scoredRecipes.sort(Comparator.comparing((Map<String, Object> recipe) -> 
                    ((Number) recipe.get("score")).doubleValue()).reversed());
            logger.debug("食谱排序耗时: {}ms", System.currentTimeMillis() - startTime);
            
            // 应用多样性过滤算法，确保推荐的食谱具有足够多样性
            startTime = System.currentTimeMillis();
            List<Map<String, Object>> diverseRecipes = ensureRecipeDiversity(scoredRecipes, count);
            logger.debug("多样性过滤耗时: {}ms", System.currentTimeMillis() - startTime);
            
            logger.debug("为用户推荐了 {} 个多样化食谱", diverseRecipes.size());
            
            // 将推荐结果存入缓存
            recipeCacheService.putToCache(cacheKey, diverseRecipes);
            
            // 返回缓存副本，避免直接修改缓存内容
            return new ArrayList<>(diverseRecipes);
        } catch (Exception e) {
            logger.error("推荐食谱过程中发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("推荐食谱失败，请稍后重试", e);
        }
    }
    
    /**
     * 提取营养素数据
     */
    private void extractNutrients(Map<String, Double> nutrients, Map<?, ?> nutrientsMap) {
        try {
            Object proteinObj = nutrientsMap.get("protein");
            if (proteinObj instanceof Number) {
                nutrients.put("protein", ((Number) proteinObj).doubleValue());
            } else {
                nutrients.put("protein", 50.0); // 默认蛋白质
            }
            
            Object carbsObj = nutrientsMap.get("carbohydrate");
            if (carbsObj instanceof Number) {
                nutrients.put("carbohydrate", ((Number) carbsObj).doubleValue());
            } else {
                nutrients.put("carbohydrate", 250.0); // 默认碳水
            }
            
            Object fatObj = nutrientsMap.get("fat");
            if (fatObj instanceof Number) {
                nutrients.put("fat", ((Number) fatObj).doubleValue());
            } else {
                nutrients.put("fat", 65.0); // 默认脂肪
            }
        } catch (Exception e) {
            logger.warn("营养素数据解析异常，使用默认值: {}", e.getMessage());
            setDefaultNutrients(nutrients);
        }
    }
    
    /**
     * 设置默认营养素值
     */
    private void setDefaultNutrients(Map<String, Double> nutrients) {
        nutrients.put("protein", 50.0);
        nutrients.put("carbohydrate", 250.0);
        nutrients.put("fat", 65.0);
    }
    
    /**
     * 根据餐别和健康目标智能分配卡路里
     */
    private double calculateMealCalories(double recommendedCalories, String mealType, String goal) {
        double mealCalories;
        
        // 基础餐卡路里分配
        if ("早餐".equals(mealType)) {
            // 优化：根据目标调整早餐比例
            if ("减重".equals(goal)) {
                mealCalories = recommendedCalories * 0.30; // 减重时早餐比例提高
            } else {
                mealCalories = recommendedCalories * 0.25; // 标准早餐25%
            }
        } else if ("午餐".equals(mealType)) {
            mealCalories = recommendedCalories * 0.35; // 午餐35%
        } else if ("晚餐".equals(mealType)) {
            // 优化：根据目标调整晚餐比例
            if ("减重".equals(goal)) {
                mealCalories = recommendedCalories * 0.25; // 减重时晚餐比例降低
            } else {
                mealCalories = recommendedCalories * 0.30; // 标准晚餐30%
            }
        } else if ("加餐".equals(mealType)) {
            mealCalories = recommendedCalories * 0.10; // 加餐10%
        } else {
            mealCalories = recommendedCalories / 3; // 默认三餐均分
        }
        
        // 确保餐卡路里为合理值
        if (mealCalories <= 0) {
            mealCalories = 500.0;
            logger.warn("餐卡路里计算结果异常，使用默认值: {}", mealCalories);
        }
        
        return mealCalories;
    }
    
    /**
     * 根据餐别获取食谱（可以扩展为更复杂的筛选逻辑）
     */
    private List<Recipe> getRecipesByMealType(String mealType) {
        // 实际应用中可以通过更复杂的参数传递给mapper进行预筛选
        // 这里简化处理，获取所有食谱后在内存中进一步筛选
        List<Recipe> allRecipes = recipeMapper.selectAll(null);
        
        // 可以根据食谱名称或标签进行简单的餐别筛选
        // 这里暂时返回所有食谱，在后续处理中进行筛选
        return allRecipes;
    }
    
    /**
     * 并行计算食谱得分，提升性能
     */
    private List<Map<String, Object>> calculateRecipeScoresParallel(List<Recipe> recipes, double mealCalories, 
            double mealProtein, double mealCarbs, double mealFat) {
        // 对于大量食谱数据，使用并行流处理
        // 对于少量数据，直接串行处理避免并行开销
        if (recipes.size() > 50) {
            return recipes.parallelStream()
                    .filter(Objects::nonNull)
                    .map(recipe -> {
                        try {
                            double score = calculateRecipeScore(recipe, mealCalories, mealProtein, mealCarbs, mealFat);
                            Map<String, Object> scoredRecipe = new HashMap<>();
                            scoredRecipe.put("recipe", recipe);
                            scoredRecipe.put("score", score);
                            scoredRecipe.put("nutritionProfile", createNutritionProfile(recipe)); // 用于多样性计算
                            return scoredRecipe;
                        } catch (Exception e) {
                            logger.warn("计算食谱得分时出错，食谱ID: {}, 错误: {}", recipe.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            // 少量数据串行处理
            List<Map<String, Object>> scoredRecipes = new ArrayList<>(recipes.size());
            for (Recipe recipe : recipes) {
                if (recipe == null) {
                    continue;
                }
                
                try {
                    double score = calculateRecipeScore(recipe, mealCalories, mealProtein, mealCarbs, mealFat);
                    Map<String, Object> scoredRecipe = new HashMap<>();
                    scoredRecipe.put("recipe", recipe);
                    scoredRecipe.put("score", score);
                    scoredRecipe.put("nutritionProfile", createNutritionProfile(recipe)); // 用于多样性计算
                    scoredRecipes.add(scoredRecipe);
                } catch (Exception e) {
                    logger.warn("计算食谱得分时出错，食谱ID: {}, 错误: {}", recipe.getId(), e.getMessage());
                }
            }
            return scoredRecipes;
        }
    }
    
    /**
     * 创建食谱的营养特征描述，用于多样性计算
     */
    private Map<String, Double> createNutritionProfile(Recipe recipe) {
        Map<String, Double> profile = new HashMap<>();
        
        if (recipe.getCalories() > 0 && recipe.getProtein() != null && 
            recipe.getCarbohydrate() != null && recipe.getFat() != null) {
            
            double calories = recipe.getCalories();
            double protein = recipe.getProtein().doubleValue();
            double carbs = recipe.getCarbohydrate().doubleValue();
            double fat = recipe.getFat().doubleValue();
            
            // 计算各营养素的卡路里贡献比例
            double proteinCalories = protein * 4;
            double carbsCalories = carbs * 4;
            double fatCalories = fat * 9;
            
            profile.put("proteinRatio", proteinCalories / calories);
            profile.put("carbsRatio", carbsCalories / calories);
            profile.put("fatRatio", fatCalories / calories);
            profile.put("calorieDensity", calories / (protein + carbs + fat)); // 简化的卡路里密度
        } else {
            // 默认值
            profile.put("proteinRatio", 0.25);
            profile.put("carbsRatio", 0.50);
            profile.put("fatRatio", 0.25);
            profile.put("calorieDensity", 4.0);
        }
        
        return profile;
    }
    
    /**
     * 确保推荐的食谱具有足够多样性的算法
     * 使用贪心算法：先选择最高分的食谱，然后选择与已选食谱差异最大的食谱
     */
    private List<Map<String, Object>> ensureRecipeDiversity(List<Map<String, Object>> scoredRecipes, int targetCount) {
        // 确保count为合理值
        int resultCount = Math.max(1, Math.min(targetCount, 100)); // 限制最多返回100个结果
        
        // 如果食谱数量少于结果数量，直接返回所有食谱
        if (scoredRecipes.size() <= resultCount) {
            return scoredRecipes;
        }
        
        List<Map<String, Object>> diverseRecipes = new ArrayList<>(resultCount);
        
        // 第一步：选择得分最高的食谱作为起始点
        diverseRecipes.add(scoredRecipes.get(0));
        
        // 贪心选择算法：每次选择与已选食谱集合差异最大的食谱
        while (diverseRecipes.size() < resultCount && diverseRecipes.size() < scoredRecipes.size()) {
            double maxDiversity = -1;
            Map<String, Object> bestCandidate = null;
            
            for (Map<String, Object> candidate : scoredRecipes) {
                // 跳过已经选择的食谱
                if (diverseRecipes.contains(candidate)) {
                    continue;
                }
                
                // 计算候选食谱与已选食谱集合的平均多样性距离
                double avgDiversity = calculateAverageDiversity(candidate, diverseRecipes);
                
                // 同时考虑分数和多样性，使用加权评分
                double score = ((Number) candidate.get("score")).doubleValue();
                double weightedScore = score * 0.7 + avgDiversity * 0.3; // 分数权重70%，多样性权重30%
                
                if (weightedScore > maxDiversity) {
                    maxDiversity = weightedScore;
                    bestCandidate = candidate;
                }
            }
            
            // 添加最佳候选食谱
            if (bestCandidate != null) {
                diverseRecipes.add(bestCandidate);
            } else {
                // 如果没有找到合适的候选，直接添加下一个高分食谱
                for (Map<String, Object> recipe : scoredRecipes) {
                    if (!diverseRecipes.contains(recipe)) {
                        diverseRecipes.add(recipe);
                        break;
                    }
                }
            }
        }
        
        return diverseRecipes;
    }
    
    /**
     * 计算候选食谱与已选食谱集合的平均多样性距离
     */
    private double calculateAverageDiversity(Map<String, Object> candidate, List<Map<String, Object>> selectedRecipes) {
        double totalDiversity = 0;
        
        @SuppressWarnings("unchecked")
        Map<String, Double> candidateProfile = (Map<String, Double>) candidate.get("nutritionProfile");
        
        for (Map<String, Object> selected : selectedRecipes) {
            @SuppressWarnings("unchecked")
            Map<String, Double> selectedProfile = (Map<String, Double>) selected.get("nutritionProfile");
            
            // 计算营养特征的欧氏距离作为多样性度量
            double proteinDiff = candidateProfile.get("proteinRatio") - selectedProfile.get("proteinRatio");
            double carbsDiff = candidateProfile.get("carbsRatio") - selectedProfile.get("carbsRatio");
            double fatDiff = candidateProfile.get("fatRatio") - selectedProfile.get("fatRatio");
            double densityDiff = candidateProfile.get("calorieDensity") - selectedProfile.get("calorieDensity");
            
            // 计算欧氏距离
            double distance = Math.sqrt(proteinDiff * proteinDiff + carbsDiff * carbsDiff + 
                                       fatDiff * fatDiff + densityDiff * densityDiff * 0.1); // 密度差异权重降低
            
            totalDiversity += distance;
        }
        
        // 返回平均距离（归一化到0-100范围）
        return Math.min(100, (totalDiversity / selectedRecipes.size()) * 200); // 缩放因子使距离值更有区分度
    }
    
    /**
     * 内部类，用于存储带评分的食谱
     */
    private static class ScoredRecipe {
        private Recipe recipe;
        private double score;
        
        public double getScore() {
            return score;
        }
        
        public Recipe getRecipe() {
            return recipe;
        }
    }

    /**
     * 计算食谱与用户需求的匹配得分（0-100）
     * 优化版：增加食物多样性、营养均衡性和健康因子权重
     * @param recipe 食谱
     * @param targetCalories 目标热量
     * @param targetProtein 目标蛋白质
     * @param targetCarbs 目标碳水化合物
     * @param targetFat 目标脂肪
     * @return 匹配得分
     */
    private double calculateRecipeScore(Recipe recipe, double targetCalories, double targetProtein, 
                                       double targetCarbs, double targetFat) {
        // 安全检查，避免空指针异常
        if (recipe == null) {
            return 0.0;
        }
        
        // 确保目标值非负，避免除零
        targetCalories = Math.max(1, targetCalories);
        targetProtein = Math.max(1, targetProtein);
        targetCarbs = Math.max(1, targetCarbs);
        targetFat = Math.max(1, targetFat);
        
        // 基础分数计算（50%权重）
        double baseScore = calculateBaseScore(recipe, targetCalories, targetProtein, targetCarbs, targetFat);
        
        // 食物多样性分数（20%权重）
        double diversityScore = calculateDiversityScore(recipe) * 0.2;
        
        // 营养均衡性分数（20%权重）
        double balanceScore = calculateBalanceScore(recipe) * 0.2;
        
        // 健康因子分数（10%权重）
        double healthScore = calculateHealthFactorScore(recipe) * 0.1;
        
        // 总分，确保得分在0-100范围内
        double totalScore = baseScore + diversityScore + balanceScore + healthScore;
        totalScore = Math.max(0, Math.min(100, totalScore));
        
        // 四舍五入到小数点后2位
        return Math.round(totalScore * 100.0) / 100.0;
    }
    
    /**
     * 计算基础匹配分数（热量和营养素匹配）
     */
    private double calculateBaseScore(Recipe recipe, double targetCalories, double targetProtein, 
                                    double targetCarbs, double targetFat) {
        // 热量匹配度（优化：使用非线性评分，接近目标值时得分提升更快）
        double calories = recipe.getCalories() != null ? recipe.getCalories() : 0;
        double caloriesDiff = Math.abs(calories - targetCalories) / targetCalories;
        // 使用平方函数增强相似度评分的区分度
        double caloriesScore = Math.max(0, 100 - (caloriesDiff * caloriesDiff) * 200) * 0.5;
        
        // 营养素匹配度（优化：分别计算各营养素权重）
        double proteinScore = 0, carbsScore = 0, fatScore = 0;
        
        if (recipe.getProtein() != null) {
            double proteinDiff = Math.abs(recipe.getProtein().doubleValue() - targetProtein) / targetProtein;
            proteinScore = Math.max(0, 100 - proteinDiff * 150);
        }
        
        if (recipe.getCarbohydrate() != null) {
            double carbsDiff = Math.abs(recipe.getCarbohydrate().doubleValue() - targetCarbs) / targetCarbs;
            carbsScore = Math.max(0, 100 - carbsDiff * 100);
        }
        
        if (recipe.getFat() != null) {
            double fatDiff = Math.abs(recipe.getFat().doubleValue() - targetFat) / targetFat;
            fatScore = Math.max(0, 100 - fatDiff * 120);
        }
        
        // 加权平均营养素得分（蛋白质权重更高）
        double nutrientScore = (proteinScore * 0.4 + carbsScore * 0.3 + fatScore * 0.3) * 0.5;
        
        return caloriesScore + nutrientScore;
    }
    
    /**
     * 计算食物多样性分数
     * 根据食谱包含的食物种类数量和多样性给予加分
     */
    private double calculateDiversityScore(Recipe recipe) {
        // 假设食谱对象中可能有配料信息，这里简化处理
        // 实际应用中可以从recipe.getIngredients()或类似字段获取
        // 这里根据食谱名称和营养结构估算多样性
        
        // 基础多样性分数
        double baseDiversity = 50; // 默认中等多样性
        
        // 根据营养素比例估算食物多样性
        if (recipe.getProtein() != null && recipe.getCarbohydrate() != null && recipe.getFat() != null) {
            double protein = recipe.getProtein().doubleValue();
            double carbs = recipe.getCarbohydrate().doubleValue();
            double fat = recipe.getFat().doubleValue();
            
            // 计算营养素分布的均衡性（越均衡，可能食材越多样）
            double totalNutrients = protein + carbs + fat;
            if (totalNutrients > 0) {
                double proteinRatio = protein / totalNutrients;
                double carbsRatio = carbs / totalNutrients;
                double fatRatio = fat / totalNutrients;
                
                // 营养素比例越均衡，多样性分数越高
                double balanceFactor = 1.0 - (Math.abs(proteinRatio - 0.3) + 
                                           Math.abs(carbsRatio - 0.5) + 
                                           Math.abs(fatRatio - 0.2));
                baseDiversity += balanceFactor * 30;
            }
        }
        
        // 限制分数范围
        return Math.max(0, Math.min(100, baseDiversity));
    }
    
    /**
     * 计算营养均衡性分数
     * 评估宏量营养素的比例是否符合健康标准
     */
    private double calculateBalanceScore(Recipe recipe) {
        double score = 60; // 基础分
        
        if (recipe.getCalories() > 0 && recipe.getProtein() != null && 
            recipe.getCarbohydrate() != null && recipe.getFat() != null) {
            
            double calories = recipe.getCalories();
            double protein = recipe.getProtein().doubleValue();
            double carbs = recipe.getCarbohydrate().doubleValue();
            double fat = recipe.getFat().doubleValue();
            
            // 计算各营养素的卡路里贡献
            double proteinCalories = protein * 4;
            double carbsCalories = carbs * 4;
            double fatCalories = fat * 9;
            
            // 计算各营养素占总卡路里的比例
            double proteinRatio = proteinCalories / calories;
            double carbsRatio = carbsCalories / calories;
            double fatRatio = fatCalories / calories;
            
            // 评估蛋白质比例（健康范围：15%-35%）
            if (proteinRatio >= 0.15 && proteinRatio <= 0.35) {
                score += 15;
            } else if (proteinRatio >= 0.1 && proteinRatio <= 0.4) {
                score += 10;
            }
            
            // 评估碳水化合物比例（健康范围：45%-65%）
            if (carbsRatio >= 0.45 && carbsRatio <= 0.65) {
                score += 15;
            } else if (carbsRatio >= 0.4 && carbsRatio <= 0.7) {
                score += 10;
            }
            
            // 评估脂肪比例（健康范围：20%-35%）
            if (fatRatio >= 0.2 && fatRatio <= 0.35) {
                score += 10;
            } else if (fatRatio >= 0.15 && fatRatio <= 0.4) {
                score += 5;
            }
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * 计算健康因子分数
     * 根据食谱的健康属性给予加分（如富含膳食纤维、低糖、低饱和脂肪等）
     */
    private double calculateHealthFactorScore(Recipe recipe) {
        // 基础健康分数
        double healthScore = 50;
        
        // 根据卡路里密度调整（低卡路里密度通常更健康）
        // 注意：这里只是简化示例，实际应用中需要更多参数
        
        // 低热量食物加分
        if (recipe.getCalories() > 0 && recipe.getCalories() < 300) {
            healthScore += 20;
        }
        
        // 高蛋白低脂肪食物加分
        if (recipe.getProtein() != null && recipe.getFat() != null && 
            recipe.getFat().doubleValue() > 0) {
            double proteinFatRatio = recipe.getProtein().doubleValue() / recipe.getFat().doubleValue();
            if (proteinFatRatio > 2.0) {
                healthScore += 20;
            } else if (proteinFatRatio > 1.5) {
                healthScore += 10;
            }
        }
        
        // 低碳水食物适当加分（但不过度强调）
        if (recipe.getCarbohydrate() != null && recipe.getCalories() > 0) {
            double carbCalories = recipe.getCarbohydrate().doubleValue() * 4;
            double carbRatio = carbCalories / recipe.getCalories();
            if (carbRatio < 0.4) {
                healthScore += 10;
            }
        }
        
        return Math.max(0, Math.min(100, healthScore));
    }

    /**
     * 生成用户的完整膳食计划
     * @param user 用户对象
     * @return 包含各餐推荐的完整膳食计划
     */
    public Map<String, List<Map<String, Object>>> generateMealPlan(User user) {
        Integer userId = user.getId();
        logger.debug("开始为用户生成完整膳食计划: 用户ID={}", userId);
        Map<String, List<Map<String, Object>>> mealPlan = new HashMap<>();
        
        // 为每餐推荐食谱
        mealPlan.put("早餐", recommendRecipes(user, "早餐", 3));
        mealPlan.put("午餐", recommendRecipes(user, "午餐", 3));
        mealPlan.put("晚餐", recommendRecipes(user, "晚餐", 3));
        mealPlan.put("加餐", recommendRecipes(user, "加餐", 2));
        
        return mealPlan;
    }
    
    /**
     * 清除用户相关缓存
     * 当用户健康数据更新时调用，确保推荐结果的准确性
     * @param user 用户对象
     */
    public void clearUserCache(User user) {
        if (user != null && user.getId() != null) {
            Integer userId = user.getId();
            recipeCacheService.clearUserCache(userId);
            logger.debug("用户健康数据更新，已清除缓存: 用户ID={}", userId);
        }
    }
}