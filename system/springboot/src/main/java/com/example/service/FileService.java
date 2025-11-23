package com.example.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 上传文件
     * @param file 要上传的文件
     * @return 上传后的文件访问URL
     * @throws IOException 上传过程中发生的IO异常
     */
    String uploadFile(MultipartFile file) throws IOException;

    /**
     * 下载文件
     * @param fileName 文件名
     * @return 文件字节数组
     * @throws IOException 下载过程中发生的IO异常
     */
    byte[] downloadFile(String fileName) throws IOException;

    /**
     * 删除文件
     * @param fileName 文件名
     * @return 是否删除成功
     */
    boolean deleteFile(String fileName);
}