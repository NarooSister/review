package com.sparta.review.service;

import com.sparta.review.entity.Product;
import com.sparta.review.dto.ReviewDto;
import com.sparta.review.repository.ProductRepository;
import com.sparta.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReviewServiceConcurrencyTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.updateCountAndScore(0L, 0.0);
        productRepository.saveAndFlush(testProduct);  // flush를 사용하여 즉시 커밋
    }
    @Test
    @DisplayName("대규모 트래픽으로 리뷰 생성 시 제대로 수행되는지")
    void createReview_HighTraffic_UpdatesReviewStatsCorrectly() throws InterruptedException {
        int numberOfThreads = 500;  // 동시에 리뷰를 작성할 사용자 수
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);  // 모든 스레드가 완료될 때까지 기다리기 위한 latch

        // 동시 리뷰 작성
        for (int i = 1; i <= numberOfThreads; i++) {
            Long userId = (long) i;  // 각기 다른 유저 아이디 생성
            executorService.execute(() -> {
                try {
                    ReviewDto reviewDto = ReviewDto.builder()
                            .userId(userId)
                            .score(5)  // 모든 리뷰의 점수는 5로 설정
                            .content("Great product!")
                            .build();
                    reviewService.createReview(testProduct.getId(), reviewDto, null);
                } catch (Exception e) {
                    System.out.println("예외 발생: " + e.getMessage());
                } finally {
                    latch.countDown();  // 스레드 완료 시 latch 감소
                }
            });
        }

        latch.await(1, TimeUnit.MINUTES);  // 모든 스레드가 완료될 때까지 대기
        executorService.shutdown();

        // 리뷰 수와 평균 점수가 올바르게 업데이트되었는지 확인
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();

        long expectedReviewCount = 0L + numberOfThreads;  // 초기 리뷰 수에 동시 요청 수를 더함
        double expectedAverageScore = (5.0 * numberOfThreads) / expectedReviewCount;  // 모든 리뷰의 평균 점수

        assertEquals(expectedReviewCount, updatedProduct.getReviewCount(), "리뷰 수가 올바르지 않습니다.");
        assertEquals(expectedAverageScore, updatedProduct.getScore(), 0.01, "리뷰 점수가 올바르지 않습니다.");
    }
    @Test
    void testConcurrentReviewCreationWithRetry() throws InterruptedException {
        int numberOfThreads = 10;  // 동시에 리뷰를 작성할 사용자 수
        int scorePerReview = 5;    // 모든 리뷰의 점수는 동일하게 5로 설정
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        List<Long> userIds = new ArrayList<>();
        for (int i = 1; i <= numberOfThreads; i++) {
            userIds.add((long) i);  // 각기 다른 유저 아이디 생성
        }

        // 동시 리뷰 작성
        for (Long userId : userIds) {
            executorService.submit(() -> {
                try {
                    ReviewDto reviewDto = ReviewDto.builder()
                            .userId(userId)
                            .score(scorePerReview)
                            .content("Great product!")
                            .build();
                    reviewService.createReview(testProduct.getId(), reviewDto, null);
                } catch (Exception e) {
                    System.out.println("예외 발생: " + e.getMessage());
                }
            });
        }

        // 모든 스레드가 끝날 때까지 대기
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // 리뷰 수와 평균 점수가 올바르게 업데이트되었는지 확인
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();

        assertEquals(numberOfThreads, updatedProduct.getReviewCount(), "리뷰 수가 올바르지 않습니다.");
        assertEquals(5.0, updatedProduct.getScore(), "평균 점수가 올바르지 않습니다.");
    }
}
