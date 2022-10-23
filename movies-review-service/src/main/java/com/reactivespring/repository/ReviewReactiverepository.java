package com.reactivespring.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.reactivespring.domain.Review;

import reactor.core.publisher.Flux;

public interface ReviewReactiverepository extends ReactiveMongoRepository<Review, String> {
    Flux<Review> findReviewsByMovieInfoId(Long movieinfouid);
}
