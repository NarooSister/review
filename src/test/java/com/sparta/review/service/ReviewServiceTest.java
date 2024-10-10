package com.sparta.review.service;


import com.sparta.review.dto.ReviewDto;
import com.sparta.review.dto.ReviewResponseDto;
import com.sparta.review.entity.Product;
import com.sparta.review.entity.Review;
import com.sparta.review.repository.ProductRepository;
import com.sparta.review.repository.ReviewRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.*;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {
    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DummyS3Uploader dummyS3Uploader;

    @InjectMocks
    private ReviewService reviewService;

    private Product product;
    private ReviewDto reviewDto;

    @BeforeEach
    void setUp(){
       product = new Product(10L, 4.2);
        reviewDto = ReviewDto.builder()
                .id(1L)
                .userId(2L)
                .score(5)
                .content("테스트 리뷰 입니다.")
                .imageUrl(null) // 이미지가 없는 경우 null
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("리뷰가 존재할 때 제대로 dto를 반환하는지")
    void getReviews_Exists_ReturnResponseDto() {
        // given
        Long productId = 1L;
        Long cursor = null;
        int size = 5;
        Review review = Review.builder()
                .id(1L)
                .userId(1L)
                .productId(productId)
                .score(5)
                .content("좋은 상품입니다.")
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findAllByProductIdOrderByIdDesc(eq(productId), any())).thenReturn(List.of(review));

        // when
        ReviewResponseDto responseDto = reviewService.getReviews(productId, cursor, size);

        // then
        assertEquals(10L, responseDto.getTotalCount());
        assertEquals(4.2, responseDto.getScore());
        assertEquals(1, responseDto.getReviews().size());
        verify(productRepository, times(1)).findById(productId);
        verify(reviewRepository, times(1)).findAllByProductIdOrderByIdDesc(eq(productId), any());
    }

    @Test
    @DisplayName("존재하지 않은 상품에 대해 예외를 던지는지")
    void getReviews_ProductNotFound_ThrowsException(){
        // given


        // when


        // then
    }

    @Test
    @DisplayName("커서가 없는 경우 첫 페이지 리뷰를 조회하는지")
    void getReviews_NoCursor_ReturnFirstPageReviews(){
        // given


        // when


        // then
    }

    @Test
    @DisplayName("커서가 있는 경우 다음 페이지 리뷰를 조회하는지")
    void getReviews_WithCursor_ReturnNextPageReviews(){
        // given


        // when


        // then
    }

    @Test
    @DisplayName("존재하지 않는 상품에 대해 리뷰를 생성하면 예외를 던지는지")
    void createReview_ProductNotFound_ThrowsException(){
        // given


        // when


        // then
    }

    @Test
    @DisplayName("동일한 사용자 Id로 리뷰를 작성하면 예외를 던지는지")
    void createReview_DuplicateReview_ThrowsException(){
        // given


        // when


        // then
    }
    @Test
    @DisplayName("잘못된 점수 입력시 예외를 던지는지")
    void createReview_InvalidScore_ThrowsException(){
        // given


        // when


        // then
    }

    @Test
    @DisplayName("이미지가 없는 리뷰를 생성할 때 리뷰가 제대로 저장되는지")
    void createReview_WithoutImage_SaveReview(){
        // given


        // when


        // then
    }

    @Test
    @DisplayName("이미지가 포함된 리뷰를 생성할 때 리뷰가 제대로 저장되는지")
    void createReview_WithImage_SaveReview(){
        // given


        // when


        // then
    }


    @Test
    @DisplayName("총 리뷰 수, 평균 점수 업데이트가 제대로 수행되는지")
    void updateProductReviewStats_UpdateContAndScore(){
        // given


        // when


        // then
    }
}
