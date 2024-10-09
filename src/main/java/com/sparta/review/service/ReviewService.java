package com.sparta.review.service;

import com.sparta.review.dto.ReviewDto;
import com.sparta.review.dto.ReviewResponseDto;
import com.sparta.review.entity.Review;
import com.sparta.review.repository.ProductRepository;
import com.sparta.review.repository.ReviewRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    public ReviewResponseDto getReviews(Long productId, Long cursor, int size) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }

        List<Review> reviews;

        // 커서가 없으면 첫 페이지 조회
        Pageable pageable = PageRequest.of(0, size + 1);
        if(cursor == null){
            reviews = reviewRepository.findAllByProductIdOrderByIdDesc(productId, pageable);
        } else {    // 있으면 해당 커서보다 작은 리뷰 조회
            reviews = reviewRepository.findByProductIdAndIdLessThanOrderByIdDesc(productId, cursor, pageable);
        }

        // 리뷰가 없을 때
        if (reviews.isEmpty()) {
            return new ReviewResponseDto(0L, 0.0, null, Collections.emptyList());
        }

        // 다음 페이지 존재여부
        boolean hasNext = reviews.size() > size;

        if(hasNext) reviews.remove(reviews.size() -1);

        List<ReviewDto> reviewDtos = reviews.stream()
                .map(ReviewDto::fromEntity)
                .toList();

        Long totalCount = reviewRepository.countByProductId(productId);
        Double averageScore = reviewRepository.getAverageScoreByProductId(productId);

        Long newCursor = reviews.isEmpty() ? null : reviews.get(reviews.size()-1).getId();

        return new ReviewResponseDto(totalCount, averageScore, newCursor, reviewDtos);
    }
}
