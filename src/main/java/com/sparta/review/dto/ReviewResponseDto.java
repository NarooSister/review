package com.sparta.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReviewResponseDto {
    private Long totalCount;    // 총 리뷰 수
    private Double score;       // 평균 점수
    private Long cursor;        // 다음 페이지 커서 값
    private List<ReviewDto> reviews;  // 리뷰 목록
}
