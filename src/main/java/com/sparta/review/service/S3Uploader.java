package com.sparta.review.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface S3Uploader {
    String upload(MultipartFile file) throws IOException;
}
