package com.sparta.review.controller;

import com.sparta.review.dto.ReviewDto;
import com.sparta.review.dto.ReviewResponseDto;
import com.sparta.review.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDateTime;
import java.util.List;

@WebMvcTest(ReviewController.class)
public class ReviewControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 조회 테스트")
    void getReviews() throws Exception {
        //given
        Long productId = 1L;
        Long cursor = null;
        int size = 10;
        List<ReviewDto> reviews = List.of(
                // 최신 리뷰순으로 저장
                new ReviewDto(3L, 3L, productId, 5, "리뷰 내용 3", null, LocalDateTime.now()),
                new ReviewDto(2L, 2L, productId, 4, "리뷰 내용 2", null, LocalDateTime.now()),
                new ReviewDto(1L, 1L, productId, 5, "리뷰 내용 1", null, LocalDateTime.now())
        );
        ReviewResponseDto responseDto = new ReviewResponseDto(3L, 4.0, cursor, reviews);

        when(reviewService.getReviews(productId, cursor, size)).thenReturn(responseDto);
        //when
        mockMvc.perform(get("/products/{productId}/reviews", productId)
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(responseDto.getTotalCount()))
                .andExpect(jsonPath("$.score").value(responseDto.getScore()))
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews[0].content").value("리뷰 내용 3"))
                .andExpect(jsonPath("$.reviews[1].content").value("리뷰 내용 2"))
                .andExpect(jsonPath("$.reviews[2].content").value("리뷰 내용 1"));

        verify(reviewService, times(1)).getReviews(productId, null, size);
    }

    @Test
    @DisplayName("리뷰 생성 테스트")
    void createReview() throws Exception {
        // given
        Long productId = 1L;
        ReviewDto reviewDto = new ReviewDto(1L, 1L, productId, 5, "테스트 리뷰", null, LocalDateTime.now());

        // 리뷰 DTO를 JSON으로 변환하여 multipart 요청의 "review" 부분에 포함
        MockMultipartFile reviewDtoJson = new MockMultipartFile(
                "review",   // 필드명
                "",         // 파일명 (없음)
                "application/json",
                ("{\"userId\": 1, \"productId\": 1, \"score\": 5, \"content\": \"테스트 리뷰\"}").getBytes()
        );

        doNothing().when(reviewService).createReview(eq(productId), any(ReviewDto.class), any(MultipartFile.class));

        // when & then
        mockMvc.perform(multipart("/products/{productId}/reviews", productId)
                        .file(reviewDtoJson)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().string("리뷰가 성공적으로 등록되었습니다."));

        verify(reviewService, times(1)).createReview(eq(productId), any(ReviewDto.class), eq(null));
    }
}
