package com.reactivespring.client;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.exception.MoviesInfoClientException;
import com.reactivespring.exception.MoviesInfoServerException;
import com.reactivespring.utils.RetryUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Component
public class MoviesInfoRestClient {
    
    private final WebClient webClient;

    @Value("${restClient.moviesInfoUrl}")
    private String movieInfoUrl;

    public Mono<MovieInfo> retrieveMovie(String movieId) {
        /*var retry = Retry.fixedDelay(3, Duration.ofSeconds(1))
        //retry pour juste ce type
        .filter(ex -> ex instanceof MoviesInfoServerException)
        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> 
        Exceptions.propagate(retrySignal.failure()));//laisse passer l'exception du server*/
        var url = movieInfoUrl.concat("/{id}");
        return webClient.get().uri(url, movieId)
            .retrieve()
            //handle 404 exception
            .onStatus(HttpStatus::is4xxClientError, clientResponse -> {
                if(clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)){
                    return Mono.error(new MoviesInfoClientException(
                        "There is no movie available for id "+movieId, 
                        clientResponse.statusCode().value()));
                }
                return clientResponse.bodyToMono(String.class)
                .flatMap(reponseMessage -> Mono.error(new 
                MoviesInfoClientException(reponseMessage, 
                clientResponse.statusCode().value())));
            })
            //fin handle
            .onStatus(HttpStatus::is5xxServerError, clientResponse -> {
                return clientResponse.bodyToMono(String.class)
                .flatMap(reponseMessage -> Mono.error(new 
                MoviesInfoServerException("Server Error Exception in MovieInfoService"+ reponseMessage)
                ));
            })
            .bodyToMono(
                MovieInfo.class)
                .retryWhen(RetryUtils.retrySpec())
                .log();
    }

    public Flux<MovieInfo> retrieveMovieInfoStream() {

        var url = movieInfoUrl.concat("/stream");
        /*var retrySpec = RetrySpec.fixedDelay(3, Duration.ofSeconds(1))
                .filter((ex) -> ex instanceof MoviesInfoServerException)
                .onRetryExhaustedThrow(((retryBackoffSpec, retrySignal) -> Exceptions.propagate(retrySignal.failure())));*/

        return webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, (clientResponse -> {
                    log.info("Status code : {}", clientResponse.statusCode().value());
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(response -> Mono.error(new MoviesInfoClientException(response, clientResponse.statusCode().value())));
                }))
                .onStatus(HttpStatus::is5xxServerError, (clientResponse -> {
                    log.info("Status code : {}", clientResponse.statusCode().value());
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(response -> Mono.error(new MoviesInfoServerException(response)));
                }))
                .bodyToFlux(MovieInfo.class)
                //.retry(3)
                .retryWhen(RetryUtils.retrySpec())
                .log();

    }

    
}
