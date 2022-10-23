package com.reactivespring.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.reactivespring.domain.Review;
import com.reactivespring.handler.ReviewHandler;
import com.reactivespring.handler.exceptionhandler.GlobalErrorHandler;
import com.reactivespring.repository.ReviewReactiverepository;
import com.reactivespring.router.ReviewRouter;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest
@ContextConfiguration(classes = { ReviewRouter.class, ReviewHandler.class, GlobalErrorHandler.class })
@AutoConfigureWebTestClient
public class ReviewsUnitTest {

    @MockBean
    private ReviewReactiverepository repository;

    @Autowired
    private WebTestClient webTestClient;
    static String REVIEWS_URL = "/v1/reviews";

    static List<Review> list = new ArrayList<Review>();

    static {
        list = List.of(
                new Review(null, 1L, "Awesome Movie", 9.0),
                new Review(null, 1L, "Awesome Movie1", 9.0),
                new Review(null, 2L, "Excellent Movie", 8.0));
    }

    @Test
    void addReview() {
        // given
        var review = new Review(null, 1L, "Awesome Movie", 9.0);
        when(repository.save(review))
                .thenReturn(Mono.just(new Review("abc", 1L, "Awesome Movie", 9.0)));
        // when
        webTestClient
                .post()
                .uri(REVIEWS_URL)
                .bodyValue(review)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Review.class)
                .consumeWith(reviewResponse -> {
                    var savedReview = reviewResponse.getResponseBody();
                    assert savedReview != null;
                    assertNotNull(savedReview.getReviewId());
                });
    }

    @Test
    void addReview_badRequest() {
        // given
        var review = new Review(null, null, "Awesome Movie", -9.0);
        when(repository.save(review))
                .thenReturn(Mono.just(new Review("abc", 1L, "Awesome Movie", 9.0)));
        // when
        webTestClient
                .post()
                .uri(REVIEWS_URL)
                .bodyValue(review)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .isEqualTo("rating.movieInfoId must not be null, rating.negative : rating is negative and please pass a non-negative value");
    }

    @Test
    void testGetAllReviews() {
        //given
        when(repository.findAll())
                .thenReturn(Flux.fromIterable(list));
        //when
        webTestClient
                .get()
                .uri(REVIEWS_URL)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Review.class)
                .value(reviews -> {
                    assertEquals(3, reviews.size());
                });
    }

    @Test
    void testUpdateReview() {
        // given
        var reviewUpdate = new Review(null, 1L, "Not an Awesome Movie", 8.0);
        when(repository.save(isA(Review.class)))
                .thenReturn(Mono.just(new Review("abc", 1L, "Not an Awesome Movie", 8.0)));
        when(repository.findById(anyString()))
                .thenReturn(Mono.just(new Review("abc", 1L, "Awesome Movie", 9.0)));
        // when
        webTestClient
                .put()
                .uri("/v1/reviews/{id}", "abc")
                .bodyValue(reviewUpdate)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Review.class)
                .consumeWith(reviewResponse -> {
                    var updatedReview = reviewResponse.getResponseBody();
                    assert updatedReview != null;
                    assertEquals(8.0, updatedReview.getRating());
                    assertEquals("Not an Awesome Movie", updatedReview.getComment());
                });

    }

    @Test
    void testDeleteReview() {
        // given
        var reviewId = "abc";
        when(repository.findById(anyString()))
                .thenReturn(Mono.just(new Review("abc", 1L, "Awesome Movie", 9.0)));
        when(repository.deleteById(anyString())).thenReturn(Mono.empty());

        // when
        webTestClient
                .delete()
                .uri("/v1/reviews/{id}", reviewId)
                .exchange()
                .expectStatus().isNoContent();
    }
}
