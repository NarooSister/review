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
import software.amazon.awssdk.regions.internal.util.Ec2MetadataConfigProvider;

import javax.swing.text.html.Option;

import static org.junit.jupiter.api.Assertions.*;


import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
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
       product = new Product(1L,10L, 4.2);
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
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when, then
        Exception exception = assertThrows(NoSuchElementException.class, () ->{
            reviewService.getReviews(productId, null, 5);
        });
        assertEquals("존재하지 않는 상품입니다.", exception.getMessage());
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("커서가 없는 경우 첫 페이지 리뷰를 조회하는지")
    void getReviews_NoCursor_ReturnFirstPageReviews(){
        // given
        Long productId = 1L;
        Review review = Review.builder()
                .id(1L)
                .userId(1L)
                .productId(productId)
                .score(5)
                .content("첫 번째 리뷰")
                .createdAt(LocalDateTime.now())
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findAllByProductIdOrderByIdDesc(eq(productId), any())).thenReturn(List.of(review));

        // when
        ReviewResponseDto responseDto = reviewService.getReviews(productId, null, 5);

        // then
        assertEquals(1, responseDto.getReviews().size());
        assertEquals("첫 번째 리뷰", responseDto.getReviews().get(0).getContent());
        verify(reviewRepository, times(1)).findAllByProductIdOrderByIdDesc(eq(productId), any());
    }

    @Test
    @DisplayName("커서가 있는 경우 다음 페이지 리뷰를 조회하는지")
    void getReviews_WithCursor_ReturnNextPageReviews(){
        // given
        Long productId = 1L;
        Long cursor = 5L;   // 이전페이지의 마지막리뷰가 5번
        int size =5;

        // 최신순으로 5,4,3번의 리뷰가 보여야 함
        Review review3 = Review.builder()
                .id(3L)
                .userId(1L)
                .productId(productId)
                .score(5)
                .content("세 번째 리뷰")
                .createdAt(LocalDateTime.now())
                .build();

        Review review4 = Review.builder()
                .id(4L)
                .userId(2L)
                .productId(productId)
                .score(3)
                .content("네 번째 리뷰")
                .createdAt(LocalDateTime.now())
                .build();

        Review review5 = Review.builder()
                .id(5L)
                .userId(3L)
                .productId(productId)
                .score(2)
                .content("다섯 번째 리뷰")
                .createdAt(LocalDateTime.now())
                .build();
        List<Review> reviews = List.of(review5,review4, review3);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findByProductIdAndIdLessThanOrderByIdDesc(eq(productId), eq(cursor), any())).thenReturn(reviews);

        // when
        ReviewResponseDto responseDto = reviewService.getReviews(productId, cursor, size);

        // then
        assertEquals(3, responseDto.getReviews().size()); // 리뷰 개수 확인
        assertEquals(5L, responseDto.getReviews().get(0).getId());
        assertEquals(4L, responseDto.getReviews().get(1).getId());
        assertEquals(3L, responseDto.getReviews().get(2).getId());
        verify(reviewRepository, times(1)).findByProductIdAndIdLessThanOrderByIdDesc(eq(productId), eq(cursor), any());
    }

    @Test
    @DisplayName("존재하지 않는 상품에 대해 리뷰를 생성하면 예외를 던지는지")
    void createReview_ProductNotFound_ThrowsException(){
        // given
        Long productId = 1L;
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.empty());

        // when
        Exception exception = assertThrows(NoSuchElementException.class, () -> {
            reviewService.createReview(productId, reviewDto, null);
        });

        // then
        assertEquals("존재하지 않는 상품입니다.", exception.getMessage());
        verify(productRepository, times(1)).findByIdForUpdate(productId);
    }

    @Test
    @DisplayName("동일한 사용자 Id로 리뷰를 작성하면 예외를 던지는지")
    void createReview_DuplicateReview_ThrowsException(){
        // given
        Long productId = 1L;
        Review existReview = Review.builder()
                .id(1L)
                .userId(2L)
                .productId(productId)
                .build();
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndUserId(productId, 2L)).thenReturn(true);

        // when
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(productId, reviewDto, null);
        });

        // then
        assertEquals("하나의 상품에 대해 하나의 리뷰만 작성 가능합니다.", exception.getMessage());
        verify(reviewRepository, times(1)).existsByProductIdAndUserId(productId, 2L);
    }

    @Test
    @DisplayName("잘못된 점수 입력시 예외를 던지는지")
    void createReview_InvalidScore_ThrowsException(){
        // given
        Long productId = 1L;
        ReviewDto invalidReviewDto = ReviewDto.builder()
                .id(reviewDto.getId())
                .userId(reviewDto.getUserId())
                .productId(reviewDto.getProductId())
                .score(6)
                .content(reviewDto.getContent())
                .imageUrl(reviewDto.getImageUrl())
                .createdAt(reviewDto.getCreatedAt())
                .build();
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

        // when
        Exception exception = assertThrows(IllegalArgumentException.class, () ->{
            reviewService.createReview(productId, invalidReviewDto, null);
        });

        // then
        assertEquals("1점에서 5점 사이의 점수만 줄 수 있습니다.", exception.getMessage());
        verify(productRepository, times(1)).findByIdForUpdate(productId);
    }

    @Test
    @DisplayName("이미지가 없는 리뷰를 생성할 때 리뷰가 제대로 저장되는지")
    void createReview_WithoutImage_SaveReview(){
        // given
        Long productId = 1L;
        ReviewDto imageEmptyReviewDto = ReviewDto.builder()
                .id(1L)
                .userId(2L)
                .score(5)
                .content("이미지가 없는 리뷰")
                .imageUrl(null)
                .createdAt(LocalDateTime.now())
                .build();
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

        // when
        reviewService.createReview(productId, imageEmptyReviewDto, null);

        // then
        verify(reviewRepository, times(1)).save(any(Review.class));
        assertNull(imageEmptyReviewDto.getImageUrl());
    }

    @Test
    @DisplayName("이미지가 포함된 리뷰를 생성할 때 리뷰가 제대로 저장되는지")
    void createReview_WithImage_SaveReview(){
        // given
        Long productId = 1L;
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(false);
        String imageUrl = "image.png";
        when(dummyS3Uploader.upload(multipartFile)).thenReturn(imageUrl);

        ReviewDto imageReviewDto = ReviewDto.builder()
                .id(1L)
                .userId(2L)
                .score(5)
                .content("이미지가 있는 리뷰")
                .imageUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .build();
        // when
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
        reviewService.createReview(productId, imageReviewDto, multipartFile);

        // then
        verify(dummyS3Uploader, times(1)).upload(multipartFile);
        verify(reviewRepository, times(1)).save(any(Review.class));
        assertNotNull(imageReviewDto.getImageUrl());
    }


    @Test
    @DisplayName("총 리뷰 수, 평균 점수 업데이트가 제대로 수행되는지")
    void updateProductReviewStats_UpdateContAndScore(){
        // given
        Long productId = 1L;
        // 리뷰 총 2개, 평균 점수 4인 상품 생성
        Product product = new Product(1L, 2L, 4.0);

        Review newReview = Review.builder()
                .id(3L)
                .userId(2L)
                .productId(productId)
                .score(5)
                .content("리뷰 입니다.")
                .build();

        ReviewDto newReviewDto = ReviewDto.builder()
                .userId(2L)
                .score(5)
                .content("새로운 리뷰")
                .build();

        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any(Review.class))).thenReturn(newReview);

        // when
        reviewService.createReview(productId, newReviewDto, null);

        // then
        assertEquals(3L, product.getReviewCount());
        // 평균 점수 (4*2+5)/3 = 4.33 이 되어야 함
        assertEquals(4.33, product.getScore(), 0.01);
        verify(productRepository, times(1)).save(product);
    }
}
