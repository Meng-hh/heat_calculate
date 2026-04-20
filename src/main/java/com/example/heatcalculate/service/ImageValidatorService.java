package com.example.heatcalculate.service;

import com.example.heatcalculate.exception.ImageValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 图片校验服务
 */
@Service
public class ImageValidatorService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    ));

    /**
     * 校验图片文件
     *
     * @param file 图片文件
     * @throws ImageValidationException 校验失败时抛出
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageValidationException("图片不能为空");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ImageValidationException("图片大小不能超过10MB");
        }

        // 校验文件格式
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ImageValidationException("仅支持 JPG、PNG、WEBP 格式的图片");
        }
    }
}
