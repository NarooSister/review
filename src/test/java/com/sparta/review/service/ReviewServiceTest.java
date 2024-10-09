package com.sparta.review.service;


import com.sparta.review.dto.ReviewDto;
import com.sparta.review.entity.Product;
import com.sparta.review.repository.ProductRepository;
import com.sparta.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        product = new Product(1L, 10L, 4.2);
        reviewDto = ReviewDto.builder()
                .id(1L)
                .userId(2L)
                .score(5)
                .content("This is a test review.")
                .imageUrl(null) // 이미지가 없는 경우 null
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("리뷰를 생성할 때 리뷰가 저장되는지 확인")
    void createReview_saveReview(){
        // given
        MultipartFile image = mock(MultipartFile.class);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));


        // when


        // then
    }

}
