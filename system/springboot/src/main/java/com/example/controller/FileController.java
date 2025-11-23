package com.example.controller;

import com.example.common.Result;
import com.example.service.FileService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件相关操作接口
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    /**
     * 文件上传 - 阿里云OSS实现
     */
    @PostMapping("/upload")
    public Result upload(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            
            // 调用OSS文件服务进行上传
            String url = fileService.uploadFile(file);
            logger.info("文件上传成功，URL: " + url);
            
            // 根据用户需求，返回完整的OSS URL而不是相对路径的代理URL
            // 这样保存到数据库后前端可以直接访问图片
            logger.info("返回完整OSS URL: " + url);
            
            return Result.success(url);
        } catch (IOException e) {
            logger.warn("文件上传错误: " + e.getMessage());
            return Result.error("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            logger.warn("文件上传异常: " + e.getMessage());
            return Result.error("文件上传失败");
        }
    }
    
    /**
     * 从OSS URL中提取相对路径（包括日期目录和文件名）
     * 支持处理带签名参数的URL格式
     */
    private String extractRelativePath(String ossUrl) {
        // 移除URL中的查询参数
        int queryParamIndex = ossUrl.indexOf('?');
        if (queryParamIndex > 0) {
            ossUrl = ossUrl.substring(0, queryParamIndex);
        }

        // 检查是否包含bucket域名
        String bucketName = "healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com";
        int bucketIndex = ossUrl.indexOf(bucketName);
        if (bucketIndex > 0) {
            // 从bucket名称后的斜杠开始提取路径
            int startIndex = bucketIndex + bucketName.length();
            if (startIndex < ossUrl.length()) {
                if (ossUrl.charAt(startIndex) == '/') {
                    return ossUrl.substring(startIndex + 1);
                } else {
                    return ossUrl.substring(startIndex);
                }
            }
        }
        
        // 查找是否包含日期格式的目录 (yyyy-MM-dd)
        Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}/[\\w\\-\\.]+$|\\d{4}-\\d{2}-\\d{2}$");
        Matcher matcher = datePattern.matcher(ossUrl);
        if (matcher.find()) {
            String foundPath = matcher.group();
            logger.info("通过日期模式找到路径: " + foundPath);
            return foundPath;
        }
        
        // 直接查找URL中的日期格式部分和文件名
        Pattern completePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})/([\\w\\-\\.]+)");
        Matcher completeMatcher = completePattern.matcher(ossUrl);
        if (completeMatcher.find()) {
            String datePart = completeMatcher.group(1);
            String filePart = completeMatcher.group(2);
            String resultPath = datePart + "/" + filePart;
            logger.info("通过完整模式找到路径: " + resultPath);
            return resultPath;
        }

        // 如果找不到日期部分，则尝试提取最后一个斜杠后的文件名
        int lastSlashIndex = ossUrl.lastIndexOf('/');
        if (lastSlashIndex > 0 && lastSlashIndex < ossUrl.length() - 1) {
            String fileName = ossUrl.substring(lastSlashIndex + 1);
            logger.info("提取到文件名: " + fileName);
            // 为了确保正确的路径格式，添加当前日期目录
            String currentDate = java.time.LocalDate.now().toString();
            logger.info("使用当前日期目录: " + currentDate);
            return currentDate + "/" + fileName;
        }
        
        // 兜底返回原URL（已移除查询参数）
        logger.info("使用兜底URL: " + ossUrl);
        return ossUrl;
    }
    
    /**
     * 代理图片访问，避免直接暴露OSS地址
     * 通过 /files/proxy/{relativePath} 路径访问OSS上的文件
     */
    
    /**
     * 测试直接访问特定OSS URL的端点
     */
    @GetMapping("/test-oss-access")
    public ResponseEntity<String> testOssAccess() {
        logger.info("开始测试直接访问OSS URL");
        try {
            // 直接使用用户提供的OSS URL进行测试
            String ossUrl = "https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-23/bd679219e0d042e892c8b2f37d7da0c0.png";
            logger.info("尝试直接访问OSS URL: " + ossUrl);
            
            // 调用服务层方法，测试从OSS获取文件
            byte[] fileData = fileService.downloadFile(ossUrl);
            
            if (fileData != null) {
                logger.info("成功从OSS获取文件，大小: " + fileData.length + "字节");
                return ResponseEntity.ok().body("文件访问成功，大小: " + fileData.length + "字节");
            } else {
                logger.error("从OSS获取文件失败，返回null");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("文件不存在或访问失败");
            }
        } catch (Exception e) {
            logger.error("OSS访问测试异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("访问异常: " + e.getMessage());
        }
    }
    
    /**
     * 代理图片访问，避免直接暴露OSS地址
     * 通过 /files/proxy/{relativePath} 路径访问OSS上的文件
     */
    @GetMapping("/proxy/**")
    public ResponseEntity<byte[]> proxyImage(HttpServletRequest request) {
        // 获取完整请求URI，然后提取相对路径部分
        String requestUri = request.getRequestURI();
        logger.info("收到代理请求，完整URI: " + requestUri);
        
        // 提取/fproxy/后面的部分作为相对路径
        int proxyIndex = requestUri.indexOf("/proxy/");
        if (proxyIndex == -1) {
            logger.error("无效的代理请求路径: " + requestUri);
            return ResponseEntity.badRequest().body(null);
        }
        
        String relativePath = requestUri.substring(proxyIndex + "/proxy/".length());
        logger.info("提取的相对路径: " + relativePath);
        
        try {
            // 构建完整的OSS URL，与测试端点使用相同的逻辑
            String ossFilePath = "https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/" + relativePath;
            logger.info("构建的OSS URL: " + ossFilePath);
            
            // 调用服务层方法，从OSS获取文件
            logger.info("开始从OSS下载文件...");
            byte[] fileData = fileService.downloadFile(ossFilePath);
            
            if (fileData == null) {
                logger.error("从OSS下载文件返回null: " + ossFilePath);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("成功获取文件数据，长度: " + fileData.length + "字节");
            
            // 从相对路径中提取文件名
            String fileName = relativePath.substring(relativePath.lastIndexOf("/") + 1);
            logger.info("提取的文件名: " + fileName);
            
            // 设置content type
            String contentType = getContentType(fileName);
            logger.info("文件MIME类型: " + contentType);
            
            // 返回文件内容
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(fileData.length)
                    .body(fileData);
                    
        } catch (Exception e) {
            logger.error("处理代理请求时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * 简单测试端点，用于测试基本的图片代理功能
     * 固定路径格式，便于测试和诊断
     */
    @GetMapping("/test-image-proxy")
    public ResponseEntity<byte[]> testImageProxy() {
        logger.info("收到测试图片代理请求");
        
        try {
            // 直接使用已知存在的OSS URL
            String ossFilePath = "https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-23/bd679219e0d042e892c8b2f37d7da0c0.png";
            logger.info("测试访问OSS文件: " + ossFilePath);
            
            // 调用服务层方法，从OSS获取文件
            byte[] fileData = fileService.downloadFile(ossFilePath);
            
            if (fileData == null) {
                logger.error("从OSS下载测试文件失败，返回null");
                return ResponseEntity.notFound().build();
            }
            
            logger.info("成功获取测试文件数据，长度: " + fileData.length + "字节");
            
            // 返回文件内容
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .contentLength(fileData.length)
                    .body(fileData);
                    
        } catch (Exception e) {
            logger.error("处理测试图片代理请求时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 文件下载 - 从OSS路径进行下载
     * 支持多种格式：
     * 1. 直接文件名：如 "1763713009130-红烧肉.png"
     * 2. 包含日期目录的完整OSS路径：如 "2025-11-22/b52777ab9cfd4101a86a8b5711f497af.png"
     * 3. 完整URL格式：如 "http://localhost:9091/files/download/1763713009130-红烧肉.png" 或 "https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-22/b52777ab9cfd4101a86a8b5711f497af.png"
     */
    @GetMapping("/download/{fileName:.+}")
    public void download(@PathVariable String fileName, HttpServletResponse response) {
        try {
            // 处理不同格式的文件路径
            String ossFilePath = fileName;
            
            // 情况1：如果是完整URL格式，提取文件路径部分
            if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
                // 提取URL中的文件路径部分
                if (fileName.contains("/files/download/")) {
                    // 旧格式URL：http://localhost:9091/files/download/1763713009130-红烧肉.png
                    ossFilePath = fileName.substring(fileName.lastIndexOf("/files/download/") + 17);
                } else if (fileName.contains(".com/")) {
                    // OSS完整URL格式：https://healthsystem-bucket.oss-cn-shenzhen.aliyuncs.com/2025-11-22/b52777ab9cfd4101a86a8b5711f497af.png
                    // 提取完整的文件路径（包括日期目录）
                    ossFilePath = fileName.substring(fileName.indexOf(".com/") + 5);
                }
            }
            
            // 从URL参数中提取文件名
            String downloadFileName = fileName.contains("/") ? fileName.substring(fileName.lastIndexOf("/") + 1) : fileName;
            
            // 根据文件扩展名设置正确的contentType，而不是强制下载
            String contentType = getContentType(downloadFileName);
            response.setContentType(contentType);
            
            // 如果是图片或文本等浏览器可直接显示的文件，不设置attachment，允许浏览器直接显示
            if (contentType.startsWith("image/") || contentType.startsWith("text/") || contentType.contains("pdf") || contentType.contains("json")) {
                // 对于可直接显示的文件，设置inline或不设置
                response.setHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8));
            } else {
                // 对于其他文件类型，仍然设置为下载
                response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8));
            }
            
            // 从OSS下载文件
            byte[] bytes = fileService.downloadFile(ossFilePath);
            
            ServletOutputStream os = response.getOutputStream();
            os.write(bytes);
            os.flush();
            os.close();
        } catch (IOException e) {
            logger.warn("文件下载错误: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.getWriter().write("文件下载错误: " + e.getMessage());
            } catch (IOException ex) {
                logger.warn("写入错误响应失败: " + ex.getMessage());
            }
        } catch (Exception e) {
            logger.warn("文件下载异常: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("文件下载异常: " + e.getMessage());
            } catch (IOException ex) {
                logger.warn("写入错误响应失败: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 根据文件名扩展名获取对应的contentType
     */
    private String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            default:
                return "application/octet-stream";
        }
    }
}
