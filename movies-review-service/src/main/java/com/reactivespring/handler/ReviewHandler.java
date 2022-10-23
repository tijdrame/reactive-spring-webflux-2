package com.reactivespring.handler;


import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.reactivespring.domain.Review;
import com.reactivespring.repository.ReviewReactiverepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import com.reactivespring.exception.ReviewDataException;
import com.reactivespring.exception.ReviewNotFoundException;

@RequiredArgsConstructor
@Slf4j
@Component
public class ReviewHandler {

    private final ReviewReactiverepository repository;

    private final Validator validator;

    Sinks.Many<Review> reviewSink = Sinks.many().replay().all();

    private void validate(Review review){
        var violations = validator.validate(review);
        log.info("constraintViolations: [{}]", violations);
        if(violations.size() > 0){
            var errorMsg = violations
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining(", "));
            throw new ReviewDataException(errorMsg.toString());
        }
    }

    public Mono<ServerResponse> addReview(ServerRequest request) {
        return request.bodyToMono(Review.class)// extract req
        .doOnNext(this::validate)
                .flatMap(repository::save)// save review
                .doOnNext(review -> reviewSink.tryEmitNext(review))
                .flatMap(savedReview -> {
                    return ServerResponse.status(HttpStatus.CREATED)
                            .bodyValue(savedReview);// return rev
                });

    }

    public Mono<ServerResponse> getReviews(ServerRequest request) {
        var movieInfoId = request.queryParam("movieInfoId");
        if (movieInfoId.isPresent()){
            var reviewsFlux = repository.findReviewsByMovieInfoId(Long.valueOf(movieInfoId.get()));
            return buildReviewsresponse(reviewsFlux);
        }else {
            var reviewsFlux = repository.findAll();
            return buildReviewsresponse(reviewsFlux);
        }
    }

    private Mono<ServerResponse> buildReviewsresponse(Flux<Review> reviewsFlux) {
        return ServerResponse.ok().body(reviewsFlux, Review.class);
    }

    public Mono<ServerResponse> updateReview(ServerRequest request) {
        var reviewId = request.pathVariable("id");
        var existingReview = repository.findById(reviewId)
        .switchIfEmpty(Mono.error((new ReviewNotFoundException
        (String.join("= ","Review not found for the given review id ", reviewId)))));
        return existingReview.flatMap(review -> request.bodyToMono(Review.class)
                .map(reqreview -> {
                    review.setComment(reqreview.getComment());
                    review.setRating(reqreview.getRating());
                    return review;
                })
                .flatMap(repository::save)
                .flatMap(savedreview -> ServerResponse.ok().bodyValue(savedreview)));
                //.switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> deleteReview(ServerRequest request) {
        var reviewId = request.pathVariable("id");
        var existingReview = repository.findById(reviewId);
        return existingReview.flatMap(review -> repository.deleteById(reviewId))
        .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> getReviewStream(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_NDJSON)
        .body(reviewSink.asFlux(), Review.class).log();
    }

}
