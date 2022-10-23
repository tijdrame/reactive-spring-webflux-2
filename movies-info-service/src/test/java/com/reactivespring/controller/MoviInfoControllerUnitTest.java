package com.reactivespring.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.service.MovieInfoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = MoviInfoController.class)
@AutoConfigureWebTestClient
public class MoviInfoControllerUnitTest {
    
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MovieInfoService service;

    private static String MOVIES_URL = "/v1/movieInfos";

    @Test
    void testGetAllMovies(){
        var movieinfos = List.of(new MovieInfo(null, "Batman Begins",
                2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15")),
                new MovieInfo(null, "The Dark Knight",
                        2008, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18")),
                new MovieInfo("abc", "Dark Knight Rises",
                        2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20")));

        when(service.getAll()).thenReturn(Flux.fromIterable(movieinfos));
        webTestClient.get().uri(MOVIES_URL).exchange()
        .expectStatus().is2xxSuccessful()
        .expectBodyList(MovieInfo.class)
        .hasSize(3);
    }

    @Test
    void testGetMovieById(){
        var movie = new MovieInfo("abc", "Dark Knight Rises",
        2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20"));
        var id = "abc";
        when(service.getById(id)).thenReturn(Mono.just(movie));
        webTestClient.get().uri(MOVIES_URL+"/{id}", id).exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody()
        .jsonPath("$.name").isEqualTo("Dark Knight Rises");
    }

    @Test
    void testAddMovieInfo() {
        var newMovie = new MovieInfo(null, "Batman Begins 1",
        2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));

        when(service.addMovieInfo(isA(MovieInfo.class))).thenReturn(Mono.just(new MovieInfo("mockId", "Batman Begins 1",
        2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"))));

        webTestClient.post().uri(MOVIES_URL)
        .bodyValue(newMovie).exchange()
        .expectStatus().isCreated()
        .expectBody(MovieInfo.class)
        .consumeWith(m -> {
            var saved = m.getResponseBody();
            assertAll(()-> assertNotNull(saved.getMovieInfoId()),
            ()-> assertEquals(saved.getMovieInfoId(), "mockId"));
            
        });
    }

    @Test
    void testUpdateMovieInfo() {
        var id = "abc";
        var newMovie = new MovieInfo(null, "Dark Knight Rises",
        2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));
        when(service.updateMovie(isA(String.class), isA(MovieInfo.class)))
        .thenReturn(Mono.just(new MovieInfo(id, "Dark Knight Rises 1",
        2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"))));

        webTestClient.put().uri(MOVIES_URL+"/{id}", id)
        .bodyValue(newMovie).exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(MovieInfo.class)
        .consumeWith(m -> {
            var saved = m.getResponseBody();
            assertNotNull(saved);
            assertEquals("Dark Knight Rises 1", saved.getName());
        });
    }

    @Test
    void testDeleteMovieInfo() {
        var id = "abc"; 
        when(service.deleteMoviebyId(id)).thenReturn(Mono.empty());
        webTestClient.delete().uri(MOVIES_URL+"/{id}", id)
        .exchange()
        .expectStatus().isNoContent();
    }

    @Test
    void testAddMovieInfoValidation() {
        var newMovie = new MovieInfo(null, "",
        -5, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));

        when(service.addMovieInfo(isA(MovieInfo.class))).thenReturn(Mono.just(new MovieInfo("mockId", "Batman Begins 1",
        2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"))));

        webTestClient.post().uri(MOVIES_URL)
        .bodyValue(newMovie).exchange()
        .expectStatus().isBadRequest()
        .expectBody(String.class)
        .consumeWith(m -> {
            var str = m.getResponseBody();
            assertAll(()-> assertNotNull(str),
            ()-> assertEquals(str, "Name must be present,Year must be a positive value"));
            
        });
    }
}
