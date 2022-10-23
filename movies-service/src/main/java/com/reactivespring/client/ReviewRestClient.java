package com.reactivespring.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.reactivespring.domain.Review;
import com.reactivespring.exception.ReviewsClientException;
import com.reactivespring.exception.ReviewsServerException;
import com.reactivespring.utils.RetryUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReviewRestClient {
    
    private final WebClient webClient;

    @Value("${restClient.reviewsUrl}")
    private String reviewsUrl;

    public Flux<Review> retrieveReviews(String movieId) {
        var url = UriComponentsBuilder.fromHttpUrl(reviewsUrl)
        .queryParam("movieInfoId", movieId)
        .buildAndExpand().toString();
        return webClient.get().uri(url)
        .retrieve()
        //handle 404 exception
        .onStatus(HttpStatus::is4xxClientError, clientResponse -> {
            if(clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)){
                return Mono.empty();
                //un film peut ne pas avoir de review
            }
            return clientResponse.bodyToMono(String.class)
            .flatMap(reponseMessage -> Mono.error(new 
            ReviewsClientException(reponseMessage)));
        })
        //fin handle
        .onStatus(HttpStatus::is5xxServerError, clientResponse -> {
            return clientResponse.bodyToMono(String.class)
            .flatMap(reponseMessage -> Mono.error(new 
            ReviewsServerException("Server Error Exception in ReviewsService "+ reponseMessage)
            ));
        })
        .bodyToFlux(Review.class)
        .retryWhen(RetryUtils.retrySpec());
    }
}
