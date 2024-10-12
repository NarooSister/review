# 💡 과제 1. 리뷰 서비스
항해 99 취업 리부트 코스에서 진행한 기업 과제입니다.

## 실행 방법

1. Git clone
   
    ```
    git clone https://github.com/NarooSister/review.git
    ```
2. 도커 실행
    ```
    docker compose up --build
    ```
3. API 엔드포인트
   - 리뷰 조회 API
     
   ```
   GET /products/{productId}/reviews?cursor={cursor}&size={size}
   ```
   
   - 리뷰 등록 API
     
   ```
   POST /products/{productId}/reviews
   ```

## 프로젝트 소개

기간: 2024.10.09 ~ 2024.10.11

## 1. 개발 환경
- Back-end : Java 17, Spring Boot 3.3.5, Spring Data JPA
- Database : MySQL 9.0
- infra : docker

## 2. 주요 기능

### 비즈니스 요구 사항
- 리뷰는 존재하는 상품에만 작성할 수 있습니다.
- 유저는 하나의 상품에 대해 하나의 리뷰만 작성 가능합니다.
- 유저는 1~5점 사이의 점수와 리뷰를 남길 수 있습니다.
- 사진은 선택적으로 업로드 가능합니다.
  - 사진은 S3에 저장된다고 가정하고, S3 적재 부분은 dummy 구현체를 생성합니다.
- 리뷰는 '가장 최근에 작성된 리뷰' 순서대로 조회합니다.

### 기술적 요구 사항
- 상품 테이블에 reviewCount와 score가 잘 반영되어야 한다.
- (Optional) 동시성을 고려한 설계를 해주세요. 많은 유저들이 동시에 리뷰를 작성할 때, 발생할 수 있는 문제를 고려해보세요.
- (Optional) 테스트 코드를 작성하면 좋습니다.

<details>
<summary>API 스펙</summary>
<div markdown="1">

### 리뷰 조회 API
- GET /products/{productId}/reviews?cursor={cursor}&size={size}

**RequestParam**
- productId : 상품 아이디
- cursor : 커서 값(직전 조회 API의 응답으로 받은 cursor 값)
- size : 조회 사이즈(default = 10)

**Response Body**

```json
{
    "totalCount": 15, // 해당 상품에 작성된 총리뷰 수
    "score": 4.6, // 평균 점수
    "cursor": 6,
    "reviews": [
       {
            "id": 15,
            "userId": 1, // 작성자 유저 아이디
            "score": 5,
            "content": "이걸 사용하고 제 인생이 달라졌습니다.",
            "imageUrl": "/image.png",
            "createdAt": "2024-11-25T00:00:00.000Z"
       },
       {
            "id": 14,
            "userId": 3, // 작성자 유저 아이디
            "score": 5,
            "content": "이걸 사용하고 제 인생이 달라졌습니다.",
            "imageUrl": null,
            "createdAt": "2024-11-24T00:00:00.000Z"
       }
    ]
}
```
<br>


### 리뷰 등록 API
- POST /products/{productId}/reviews

**Request Part**

[이미지 파일]

MultipartFile 타입의 단건 이미지

[요청부]

```json
{
    "userId": 1,
    "score": 4,
    "content": "이걸 사용하고 제 인생이 달라졌습니다."
}
```

**Response Body**

NONE.

<br>
</div>
</details>

## 3. 기능 구현

### ERD
<img align="center"><img src ="img/ERD.png"></img></p>


### 리뷰 조회 API
- GET /products/{productId}/reviews?cursor={cursor}&size={size}

<img align="center"><img src ="img/withParamGet.png"></img></p>


### 리뷰 등록 API
- POST /products/{productId}/reviews

<img align="center"><img src ="img/PostSuccess.png"></img></p>

## 4. 문제 해결 과정 

### ✅ 동시성 문제 해결 (비관적 락)

**1. 동시에 리뷰를 생성할 때 문제 확인**

<img align="center"><img src ="img/TestFail.png"></img></p>

- 여러 스레드가 동시에 createReview()를 호출할 때 리뷰 수와 평균 점수 업데이트가 제대로 이루어지는지 테스트 ->
reviewCount와 score 모두 제대로 결과가 나오지 않음.

**2. 비관적 락 vs 낙관적 락**

리뷰 서비스의 경우에는 읽기 작업이 쓰기 작업보다 많은 것으로 예상되고, 
리뷰 작성의 특성 상 동시에 리뷰를 작성하여 트래픽이 엄청나게 몰리는 경우는 드물다고 생각했다.
처음에는 낙관적 락을 사용해서 문제를 해결하기로 했다.

**3. 낙관적 락에서 데드락 이유 찾기**

<img align="center"><img src ="img/InnoDBFail.png"></img></p>

낙관적 락으로 구현했는데도 불구하고 데드락이 발생했다.

`show engine innodb status;` 로 로그를 확인

**문제 :** 
- 트랜잭션 1과 트랜잭션 2가 모두 동일한 Product에 대해 업데이트를 시도한다.
- 트랜잭션 1이 먼저 s-lock을 획득하고, 이어서 x-lock을 요청한다.
- 트랜잭션 2도 s-lock을 획득하고, x-lock을 요청하지만, 이 때 트랜잭션 1이 대기 중이다.
- 두 트랜잭션이 데드락 상태에 빠지게 된다.

**원인 파악**
- MySQL은 pk가 존재하는 테이블에서 fk를 포함한 데이터를 삽입, 수정, 삭제하는 경우 제약 조건을 확인하기 위해 s-lock을 설정한다.
- 또한 MySQL InnoDB는 데이터를 수정할 때 항상 x-lock을 건다.
- fk 제약 조건이 있는 테이블에서는 낙관적 락을 사용할 수 없다.
- 외래키를 제거하면 되지 않을까?

**4. 외래키 관계 제거 후 계속되는 데드락**
- 외래키를 제거한 뒤에도 데드락 상황이 계속 되었다.
- 리뷰가 생성될 때 어플리케이션은 Product의 review_count와 score 필드를 업데이트한다.
- 두 트랜잭션이 동일한 데이터에 대해 s-lock을 보유하고 있고, 모두 업데이트를 위해 x-lock을 획득하려고 해서 마찬가지로 데드락이 발생한다.
- 따라서 외래 키가 없더라도 Product 테이블의 동일한 데이터를 업데이트하기 때문에 락이 필요하다.

**5. 낙관적 락이 데드락을 해결하지 못한 이유**
- 낙관적 락은 데이터 변경 시 version을 이용해 충돌을 감지한다. 그러나 DB 레벨의 lock을 제어하지는 않는다.
- InnoDB는 내부적으로 lock을 사용하므로 낙관적 락의 version 검사가 이루어지기 전에 데드락이 발생한다.
  
**6. 비관적 락 사용 후 문제 해결**
- 리뷰 생성 시 Product에 review_count와 score를 즉시 업데이트 하는 로직 자체가 Product에 대한 동시적인 업데이트 발생을 만든다.
- 따라서 로직을 변경하지 않는 이상 낙관적 락으로 해결 할 수 없어서 비관적 락을 사용했다.
- Product의 존재유무를 확인하는 `findByIdForUpdate`에서 비관적 락을 걸어줬다.
  
```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);
}
```

**7. 예상되는 문제점과 느낀 점**
- Lock의 개념과 사용에 대해 깊이 공부할 수 있는 과제였다.
- 비관적 락은 트랜잭션이 시작되면 해당 자원에 대한 lock을 먼저 걸기 때문에 성능 저하가 발생할 수 있다.
- 만약 리뷰 생성이 오래 걸리거나 다른 작업이 추가된다면 lock이 걸리는 시간이 길어져 불쾌한 사용자 경험을 유발할 수 있다.
- 지금의 방식으로는 비관적 락에서 데드락의 가능성이 여전히 있고, 트래픽이 증가하면 성능 저하가 더 두드러질 수 있다.
- 리뷰 생성과 상품 업데이트 로직을 잘 분리하면 낙관적 락으로 해결할 수 있을 것 같아서 좀 더 고민해봐야겠다.
  
- 업데이트 부분을 분리하여 스케줄러로 주기적으로 업데이트 하는 방법은 결과가 즉시 반영이 안되기 때문에 염두하지 않았는데, 그 방식을 사용했어도 동시성을 잘 제어할 수 있었을 것 같다.
