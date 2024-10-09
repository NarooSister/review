package com.sparta.review.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DummyS3Uploader implements S3Uploader {
    @Override
    public String upload(MultipartFile file) {
        return "https://dummy-s3-url.com/" + file.getOriginalFilename();
    }
}
