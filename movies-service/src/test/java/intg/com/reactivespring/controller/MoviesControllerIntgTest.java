package com.reactivespring.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Objects;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reactivespring.domain.Movie;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 8084)//demarre un server sur ce port
@TestPropertySource(
    properties = {
        "restClient.moviesInfoUrl=http://localhost:8084/v1/movieInfos",
        "restClient.reviewsUrl=http://localhost:8084/v1/reviews"
    }
)
public class MoviesControllerIntgTest {

    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void testRetrieveMovieById(){
        var movieId = "abc";
        //simuler le servie movie info
        stubFor(get(urlEqualTo("/v1/movieInfos/"+movieId))
        .willReturn(aResponse().withHeader("Content-type", "application/json")
        //fich dans resources/__files qui est au meme niv que le dossier java
        .withBodyFile("movieinfo.json")));
        //simuler le servie review
        stubFor(get(urlPathEqualTo("/v1/reviews"))//pas besoin d'aj le query param
        .willReturn(aResponse().withHeader("Content-type", "application/json")
        //fich dans resources/__files (chemin par defaut)
        .withBodyFile("reviews.json")));
        webTestClient.get()
        .uri("/v1/movies/{id}", movieId)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Movie.class)
        .consumeWith(movieExchrslt -> {
            var movie = movieExchrslt.getResponseBody();
            assert Objects.requireNonNull(movie).getReviewList().size() == 2;
            assertEquals("Batman Begins", movie.getMovieInfo().getName());
        });
    }

    @Test
    void testRetrieveMovieById_404(){
        var movieId = "abc";
        //simuler le servie movie info
        stubFor(get(urlEqualTo("/v1/movieInfos/"+movieId))
        .willReturn(aResponse()
        .withStatus(404)));
        //simuler le servie review
        stubFor(get(urlPathEqualTo("/v1/reviews"))//pas besoin d'aj le query param
        .willReturn(aResponse().withHeader("Content-type", "application/json")
        //fich dans resources/__files (chemin par defaut)
        .withBodyFile("reviews.json")));
        webTestClient.get()
        .uri("/v1/movies/{id}", movieId)
        .exchange()
        .expectStatus().is4xxClientError()
        .expectBody(String.class)
        .isEqualTo("There is no movie available for id "+movieId);
    }

    @Test
    void testRetrieveMovieById_reviews_404(){
        var movieId = "abc";
        //simuler le servie movie info
        stubFor(get(urlEqualTo("/v1/movieInfos/"+movieId))
        .willReturn(aResponse().withHeader("Content-type", "application/json")
        .withBodyFile("movieinfo.json")));
        //simuler le servie review
        stubFor(get(urlPathEqualTo("/v1/reviews"))//pas besoin d'aj le query param
        .willReturn(aResponse()
        .withStatus(404)));
        webTestClient.get()
        .uri("/v1/movies/{id}", movieId)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Movie.class)
        .consumeWith(movieExchrslt -> {
            var movie = movieExchrslt.getResponseBody();
            assert Objects.requireNonNull(movie).getReviewList().size() == 0;
            assertEquals("Batman Begins", movie.getMovieInfo().getName());
        });
    }

    @Test
    void testRetrieveMovieById_5XX(){
        var movieId = "abc";
        //simuler le servie movie info
        stubFor(get(urlEqualTo("/v1/movieInfos/"+movieId))
        .willReturn(aResponse()
        .withStatus(500).withBody("MoviInfo Service Unavailable")));
        //PAS BESOIN de simuler le servie review (optionel)
        //stubFor(get(urlPathEqualTo("/v1/reviews"))//pas besoin d'aj le query param
        //.willReturn(aResponse().withHeader("Content-type", "application/json")
        //fich dans resources/__files (chemin par defaut)
        //.withBodyFile("reviews.json")));
        webTestClient.get()
        .uri("/v1/movies/{id}", movieId)
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody(String.class)
        .isEqualTo("Server Error Exception in MovieInfoServiceMoviInfo Service Unavailable");

        //method static de Wiremock
        verify(4, getRequestedFor(urlEqualTo("/v1/movieInfos/"+movieId)));
    }

    @Test
    void testRetrieveMovieById_reviews_5XX(){
        var movieId = "abc";
        //simuler le servie movie info
        stubFor(get(urlEqualTo("/v1/movieInfos/"+movieId))
        .willReturn(aResponse().withHeader("Content-type", "application/json")
        .withBodyFile("movieinfo.json")));

        
        //PAS BESOIN de simuler le servie review (optionel)
        stubFor(get(urlPathEqualTo("/v1/reviews"))//pas besoin d'aj le query param
        .willReturn(aResponse()
        .withStatus(500).withBody("Review Service Not Available")));
        webTestClient.get()
        .uri("/v1/movies/{id}", movieId)
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody(String.class)
        .isEqualTo("Server Error Exception in ReviewsService Review Service Not Available");

        //method static de Wiremock
        verify(4, getRequestedFor(urlPathMatching("/v1/reviews*")));
    }
}
