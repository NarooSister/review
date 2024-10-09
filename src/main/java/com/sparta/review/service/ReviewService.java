package com.sparta.review.service;

import com.sparta.review.dto.ReviewDto;
import com.sparta.review.dto.ReviewResponseDto;
import com.sparta.review.entity.Product;
import com.sparta.review.entity.Review;
import com.sparta.review.repository.ProductRepository;
import com.sparta.review.repository.ReviewRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final DummyS3Uploader dummyS3Uploader;

    public ReviewResponseDto getReviews(Long productId, Long cursor, int size) {
        // 상품이 있는 지 확인
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new NoSuchElementException("존재하지 않는 상품입니다."));

        List<Review> reviews;

        // 커서가 없으면 첫 페이지 조회
        Pageable pageable = PageRequest.of(0, size + 1);
        if (cursor == null) {
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

        if (hasNext) reviews.remove(reviews.size() - 1);

        List<ReviewDto> reviewDtos = reviews.stream()
                .map(ReviewDto::fromEntity)
                .toList();

        Long newCursor = reviews.isEmpty() ? null : reviews.get(reviews.size() - 1).getId();

        return new ReviewResponseDto(product.getReviewCount(), product.getScore(), newCursor, reviewDtos);
    }

    @Transactional
    public void createReview(Long productId, ReviewDto reviewDto, MultipartFile multipartFile) {
        // 상품이 있는 지 확인
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new NoSuchElementException("존재하지 않는 상품입니다."));


        // 유저가 이 상품에 대해 이미 작성한 리뷰가 있는지 확인
        if (reviewRepository.existsByProductIdAndUserId(productId, reviewDto.getUserId())) {
            throw new IllegalArgumentException("하나의 상품에 대해 하나의 리뷰만 작성 가능합니다.");
        }

        Integer score = reviewDto.getScore();
        // 점수 제한
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("1점에서 5점 사이의 점수만 줄 수 있습니다.");
        }

        String imageUrl = null;
        if (multipartFile != null && !multipartFile.isEmpty()) {
            imageUrl = dummyS3Uploader.upload(multipartFile);
        }

        Review review = Review.builder()
                .userId(reviewDto.getUserId())
                .product(product)
                .score(score)
                .imageUrl(imageUrl)
                .content(reviewDto.getContent())
                .build();

        reviewRepository.save(review);
        // 총 리뷰 수, 평균 점수 업데이트
        updateProductReviewStats(product, score);
    }

    private void updateProductReviewStats(Product product, Integer newScore) {
        Long newReviewCount = product.getReviewCount() + 1;
        Double newAverageScore = ((product.getScore() * product.getReviewCount()) + newScore) / newReviewCount;

        // 총 리뷰 수, 평균 점수 업데이트
        product.updateCountAndScore(newReviewCount, newAverageScore);

        // 저장
        productRepository.save(product);
    }
}
