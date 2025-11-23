package com.example.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.OSSObjectSummary;
import com.example.config.OSSConfig;
import com.example.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阿里云OSS文件服务实现
 */
@Service
public class OssFileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(OssFileServiceImpl.class);

    @Autowired
    private OSS ossClient;

    @Autowired
    private OSSConfig ossConfig;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        // 生成唯一的文件名，避免文件覆盖
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
        String fileName = UUID.randomUUID().toString().replaceAll("-", "") + suffix;

        // 指定文件存储路径，按日期分类
        String dateDir = java.time.LocalDate.now().toString();
        String filePath = dateDir + "/" + fileName;

        // 上传文件
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossConfig.getBucketName(), filePath, inputStream);
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            // 返回文件访问URL
            return ossConfig.getFileUrl() + "/" + filePath;
        }
    }

    @Override
    public byte[] downloadFile(String fileName) throws IOException {
        logger.info("开始下载文件，请求参数: " + fileName);
        
        // 使用normalizeFilePath方法标准化路径，从不同格式的URL中提取正确的文件路径
        String ossFilePath = normalizeFilePath(fileName);
        
        // 移除可能存在的查询参数
        int queryIndex = ossFilePath.indexOf('?');
        if (queryIndex > 0) {
            ossFilePath = ossFilePath.substring(0, queryIndex);
        }
        
        logger.info("准备使用标准化路径访问OSS: " + ossFilePath);
        
        try {
            // 尝试1: 使用标准化后的完整路径获取文件
            try {
                OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), ossFilePath);
                try (InputStream inputStream = ossObject.getObjectContent()) {
                    byte[] fileData = inputStream.readAllBytes();
                    logger.info("成功从OSS获取文件: " + ossFilePath + "，文件大小: " + fileData.length + "字节");
                    return fileData;
                }
            } catch (Exception e) {
                logger.warn("直接路径访问失败，开始尝试其他查找策略: " + ossFilePath, e);
                
                // 从路径中提取文件名，用于在历史目录中查找
                String simpleFileName = ossFilePath;
                int lastSlashIndex = ossFilePath.lastIndexOf('/');
                if (lastSlashIndex > 0) {
                    simpleFileName = ossFilePath.substring(lastSlashIndex + 1);
                    logger.info("提取的简单文件名: " + simpleFileName);
                }
                
                // 尝试2: 在当前日期目录中查找任何文件（考虑到文件名可能不同的情况）
                String dateDir = java.time.LocalDate.now().toString();
                byte[] fileData = findFirstFileInDirectory(dateDir);
                if (fileData != null) {
                    logger.info("成功在当前日期目录 " + dateDir + " 中找到任意图片文件");
                    return fileData;
                }
                
                // 尝试3: 在历史目录中查找同名文件
                logger.info("开始尝试在历史日期目录中查找同名文件: " + simpleFileName);
                fileData = searchFileInDateDirectories(simpleFileName);
                if (fileData != null) {
                    logger.info("成功在历史目录中找到文件: " + simpleFileName + "，文件大小: " + fileData.length + "字节");
                    return fileData;
                }
                logger.warn("在历史目录中未找到同名文件: " + simpleFileName);
                
                // 尝试4: 在最近的几个日期目录中查找任何PNG文件（考虑到可能是不同文件名的图片）
                for (int i = 0; i < 7; i++) {
                    dateDir = java.time.LocalDate.now().minusDays(i).toString();
                    fileData = findAnyPngFileInDirectory(dateDir);
                    if (fileData != null) {
                        logger.info("成功在日期目录 " + dateDir + " 中找到任意PNG图片");
                        return fileData;
                    }
                }
                
                // 如果所有尝试都失败，抛出异常
                throw new IOException("无法从OSS获取文件: " + fileName + "（尝试了多种查找策略）", e);
            }
        } catch (IOException e) {
            logger.error("文件下载失败: " + fileName, e);
            throw e;
        }
    }
    
    /**
     * 在指定日期目录中查找第一个可用的文件
     */
    private byte[] findFirstFileInDirectory(String dateDir) {
        try {
            // 列出目录中的所有对象
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(ossConfig.getBucketName())
                    .withPrefix(dateDir + "/")
                    .withMaxKeys(1);
            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
            
            if (objectListing.getObjectSummaries().size() > 0) {
                String firstObjectKey = objectListing.getObjectSummaries().get(0).getKey();
                logger.info("在目录 " + dateDir + " 中找到第一个文件: " + firstObjectKey);
                
                // 获取第一个文件
                OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), firstObjectKey);
                try (InputStream inputStream = ossObject.getObjectContent()) {
                    return inputStream.readAllBytes();
                }
            }
        } catch (Exception e) {
            logger.warn("在目录 " + dateDir + " 中查找第一个文件失败", e);
        }
        return null;
    }
    
    /**
     * 在指定日期目录中查找任意PNG图片文件
     */
    private byte[] findAnyPngFileInDirectory(String dateDir) {
        try {
            // 列出目录中的所有对象
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(ossConfig.getBucketName())
                    .withPrefix(dateDir + "/");
            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
            
            // 查找PNG文件
            for (OSSObjectSummary summary : objectListing.getObjectSummaries()) {
                String key = summary.getKey();
                if (key.toLowerCase().endsWith(".png")) {
                    logger.info("在目录 " + dateDir + " 中找到PNG文件: " + key);
                    
                    // 获取PNG文件
                    OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), key);
                    try (InputStream inputStream = ossObject.getObjectContent()) {
                        return inputStream.readAllBytes();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("在目录 " + dateDir + " 中查找PNG文件失败", e);
        }
        return null;
    }
    
    /**
     * 标准化文件路径，从不同格式的URL中提取正确的文件路径
     */
    private String normalizeFilePath(String url) {
        // 移除URL中的查询参数部分（如果有）
        int queryParamIndex = url.indexOf('?');
        if (queryParamIndex > 0) {
            url = url.substring(0, queryParamIndex);
        }
        
        // 检查是否是完整URL
        if (url.startsWith("http://") || url.startsWith("https://")) {
            // 处理OSS域名格式的URL: https://bucket-name.oss-cn-region.aliyuncs.com/yyyy-MM-dd/filename
            if (url.contains(".com/")) {
                url = url.substring(url.indexOf(".com/") + 5);
                logger.info("从OSS URL中提取路径: " + url);
                return url;
            }
            
            // 处理应用内的下载URL: http://localhost:9091/files/download/2025-11-22/filename
            if (url.contains("/files/download/")) {
                // 确保从最后一个匹配的路径开始提取
                int startIndex = url.lastIndexOf("/files/download/") + 17;
                if (startIndex <= url.length()) {
                    String extractedPath = url.substring(startIndex);
                    logger.info("从应用下载URL中提取路径: " + extractedPath);
                    return extractedPath;
                }
            }
            
            // 处理应用内的代理URL: http://localhost:9091/files/proxy/2025-11-22/filename
            if (url.contains("/files/proxy/")) {
                // 确保从最后一个匹配的路径开始提取
                int startIndex = url.lastIndexOf("/files/proxy/") + 15;
                if (startIndex <= url.length()) {
                    String extractedPath = url.substring(startIndex);
                    logger.info("从应用代理URL中提取路径: " + extractedPath);
                    return extractedPath;
                }
            }
            
            // 对于其他URL格式，尝试提取最后一部分作为文件名
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex > 0 && lastSlashIndex < url.length() - 1) {
                String extracted = url.substring(lastSlashIndex + 1);
                logger.info("从其他URL格式提取文件名: " + extracted);
                return extracted;
            }
            
            // 如果URL中不包含日期目录，但文件名前面有日期格式的部分，尝试提取
            // 例如：http://localhost:9091/files/proxy/2025-11-23/bd679219e0d042e892c8b2f37d7da0c0.png
            String datePattern = "\\d{4}-\\d{2}-\\d{2}";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(datePattern);
            java.util.regex.Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                String dateStr = matcher.group();
                int lastSlashIndexAfterDate = url.lastIndexOf('/');
                if (lastSlashIndexAfterDate > 0 && lastSlashIndexAfterDate < url.length() - 1) {
                    String filename = url.substring(lastSlashIndexAfterDate + 1);
                    String extractedPath = dateStr + "/" + filename;
                    logger.info("从URL中提取日期和文件名: " + extractedPath);
                    return extractedPath;
                }
            }
        }
        
        // 检查是否已经是路径格式，直接返回
        return url;
    }
    
    /**
     * 搜索文件在最近30天的日期目录中
     */
    private byte[] searchFileInDateDirectories(String simpleFileName) throws IOException {
        // 遍历最近30天的日期目录
        for (int i = 0; i < 30; i++) {
            String dateDir = java.time.LocalDate.now().minusDays(i).toString();
            String tryPath = dateDir + "/" + simpleFileName;
            logger.info("尝试在历史目录查找: " + tryPath);
            
            try {
                OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), tryPath);
                try (InputStream inputStream = ossObject.getObjectContent()) {
                    logger.info("成功: 文件在历史目录 " + dateDir + " 中找到");
                    return inputStream.readAllBytes();
                }
            } catch (Exception ignored) {
                // 静默尝试，避免日志过多
            }
        }
        
        return null;
    }

    @Override
    public boolean deleteFile(String fileName) {
        // 从OSS删除文件
        ossClient.deleteObject(ossConfig.getBucketName(), fileName);
        return true;
    }
}