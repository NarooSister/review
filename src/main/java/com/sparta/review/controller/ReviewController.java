package com.sparta.review.controller;

import com.sparta.review.dto.ReviewResponseDto;
import com.sparta.review.service.ReviewService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/products/{productId}/reviews")
    public ReviewResponseDto getReviews(
            @PathVariable
            Long productId,
            @RequestParam(required = false)
            Long cursor,
            @RequestParam(defaultValue = "10")
            int size
    ) {
        return reviewService.getReviews(productId, cursor, size);
    }
}
