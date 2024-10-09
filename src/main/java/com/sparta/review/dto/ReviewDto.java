package com.sparta.review.dto;

import com.sparta.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Long id;
    private Long userId;
    private Integer score;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;

    // 정적 팩토리 메서드
    public static ReviewDto fromEntity(Review review) {
        return new ReviewDto(
                review.getId(),
                review.getUserId(),
                review.getScore(),
                review.getContent(),
                review.getImageUrl(),
                review.getCreatedAt()
        );
    }
}
