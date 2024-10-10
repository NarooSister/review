package com.sparta.review.controller;

import com.sparta.review.dto.ReviewDto;
import com.sparta.review.dto.ReviewResponseDto;
import com.sparta.review.service.ReviewService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/products/{productId}/reviews")
    public ReviewResponseDto getReviews(
            @PathVariable(name = "productId")
            Long productId,
            @RequestParam(name = "cursor", required = false)
            Long cursor,
            @RequestParam(name ="size", defaultValue = "10")
            int size
    ) {
        return reviewService.getReviews(productId, cursor, size);
    }

    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<String> createReview(
            @PathVariable(name = "productId")
            Long productId,
            @RequestPart(value = "review")
            ReviewDto reviewDto,
            @RequestPart(value = "image", required = false)
            MultipartFile image
    ){
        reviewService.createReview(productId, reviewDto, image);
        return ResponseEntity.status(201).body("리뷰가 성공적으로 등록되었습니다.");
    }
}
