package com.sparta.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long reviewCount;
    private Double score;

    public void updateCountAndScore(Long totalCount, Double averageScore) {
        this.reviewCount = totalCount;
        this.score = averageScore;
    }
}
