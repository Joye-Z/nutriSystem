package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 食谱推荐结果缓存服务
 * 用于缓存用户的食谱推荐结果，提高系统性能和响应速度
 */
@Service
public class RecipeCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecipeCacheService.class);
    
    // 缓存数据结构：使用ConcurrentHashMap保证线程安全
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    // 缓存过期时间（默认30分钟）
    private final long CACHE_EXPIRY_MINUTES = 30;
    
    // 最大缓存条目数
    private final int MAX_CACHE_ENTRIES = 1000;
    
    // 缓存清理锁
    private final ReentrantLock cleanupLock = new ReentrantLock();
    
    /**
     * 缓存条目内部类
     */
    private static class CacheEntry {
        private final List<Map<String, Object>> recommendedRecipes; // 推荐的食谱列表
        private final LocalDateTime createdAt; // 创建时间
        private final int accessCount; // 访问次数
        private final LocalDateTime lastAccessTime; // 最后访问时间
        
        public CacheEntry(List<Map<String, Object>> recommendedRecipes) {
            this.recommendedRecipes = recommendedRecipes;
            this.createdAt = LocalDateTime.now();
            this.accessCount = 1; // 初始访问次数为1
            this.lastAccessTime = LocalDateTime.now();
        }
        
        public CacheEntry(List<Map<String, Object>> recommendedRecipes, CacheEntry oldEntry) {
            this.recommendedRecipes = recommendedRecipes;
            this.createdAt = oldEntry.createdAt;
            this.accessCount = oldEntry.accessCount + 1;
            this.lastAccessTime = LocalDateTime.now();
        }
        
        public List<Map<String, Object>> getRecommendedRecipes() {
            return recommendedRecipes;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public int getAccessCount() {
            return accessCount;
        }
        
        public LocalDateTime getLastAccessTime() {
            return lastAccessTime;
        }
        
        /**
         * 判断缓存是否过期
         */
        public boolean isExpired(long expiryMinutes) {
            return ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now()) > expiryMinutes;
        }
    }
    
    /**
     * 生成缓存键
     * @param userId 用户ID
     * @param mealType 餐别
     * @param count 推荐数量
     * @return 缓存键
     */
    public String generateCacheKey(Integer userId, String mealType, int count) {
        return String.format("recipe_recommend:%d:%s:%d", userId, mealType, count);
    }
    
    /**
     * 从缓存获取推荐结果
     * @param key 缓存键
     * @return 推荐的食谱列表，如果缓存不存在或已过期则返回null
     */
    public List<Map<String, Object>> getFromCache(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            logger.debug("缓存未命中: {}", key);
            return null;
        }
        
        // 检查是否过期
        if (entry.isExpired(CACHE_EXPIRY_MINUTES)) {
            logger.debug("缓存已过期: {}", key);
            // 惰性删除过期缓存
            cache.remove(key);
            return null;
        }
        
        // 更新访问信息
        cache.put(key, new CacheEntry(entry.getRecommendedRecipes(), entry));
        
        logger.debug("缓存命中: {}, 访问次数: {}", key, entry.getAccessCount() + 1);
        return entry.getRecommendedRecipes();
    }
    
    /**
     * 将推荐结果存入缓存
     * @param key 缓存键
     * @param recommendedRecipes 推荐的食谱列表
     */
    public void putToCache(String key, List<Map<String, Object>> recommendedRecipes) {
        if (!StringUtils.hasText(key) || recommendedRecipes == null || recommendedRecipes.isEmpty()) {
            logger.warn("缓存参数无效，跳过缓存");
            return;
        }
        
        // 检查缓存大小，必要时清理
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cleanupExpiredEntries();
        }
        
        // 如果清理后仍然太大，删除访问次数最少的条目
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            evictLeastRecentlyUsed();
        }
        
        cache.put(key, new CacheEntry(recommendedRecipes));
        logger.debug("推荐结果已缓存: {}, 缓存大小: {}", key, cache.size());
    }
    
    /**
     * 清理过期的缓存条目
     */
    private void cleanupExpiredEntries() {
        if (cleanupLock.tryLock()) {
            try {
                logger.debug("开始清理过期缓存");
                int beforeSize = cache.size();
                
                cache.entrySet().removeIf(entry -> {
                    CacheEntry cacheEntry = entry.getValue();
                    return cacheEntry != null && cacheEntry.isExpired(CACHE_EXPIRY_MINUTES);
                });
                
                int afterSize = cache.size();
                logger.debug("缓存清理完成: 移除了 {} 个过期条目", beforeSize - afterSize);
            } finally {
                cleanupLock.unlock();
            }
        }
    }
    
    /**
     * 驱逐最近最少使用的缓存条目
     */
    private void evictLeastRecentlyUsed() {
        try {
            String lruKey = null;
            LocalDateTime oldestAccessTime = LocalDateTime.now();
            
            for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
                CacheEntry cacheEntry = entry.getValue();
                if (cacheEntry != null && cacheEntry.getLastAccessTime().isBefore(oldestAccessTime)) {
                    oldestAccessTime = cacheEntry.getLastAccessTime();
                    lruKey = entry.getKey();
                }
            }
            
            if (lruKey != null) {
                cache.remove(lruKey);
                logger.debug("已驱逐最近最少使用的缓存: {}", lruKey);
            }
        } catch (Exception e) {
            logger.warn("缓存驱逐过程中出错: {}", e.getMessage());
        }
    }
    
    /**
     * 清除特定用户的所有缓存
     * @param userId 用户ID
     */
    public void clearUserCache(Integer userId) {
        if (userId == null) {
            return;
        }
        
        String userPrefix = String.format("recipe_recommend:%d:", userId);
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(userPrefix));
        logger.debug("已清除用户 {} 的所有缓存", userId);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        cache.clear();
        logger.debug("已清除所有缓存");
    }
    
    /**
     * 获取当前缓存统计信息
     * @return 缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalEntries", cache.size());
        
        // 计算平均访问次数
        double avgAccessCount = cache.values().stream()
                .mapToInt(CacheEntry::getAccessCount)
                .average()
                .orElse(0);
        stats.put("averageAccessCount", avgAccessCount);
        
        // 计算缓存平均年龄（分钟）
        LocalDateTime now = LocalDateTime.now();
        double avgAgeMinutes = cache.values().stream()
                .mapToLong(entry -> ChronoUnit.MINUTES.between(entry.getCreatedAt(), now))
                .average()
                .orElse(0);
        stats.put("averageAgeMinutes", avgAgeMinutes);
        
        return stats;
    }
}