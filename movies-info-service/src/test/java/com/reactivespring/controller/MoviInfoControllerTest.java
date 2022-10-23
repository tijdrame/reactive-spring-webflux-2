package com.reactivespring.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.repository.MovieInforepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
public class MoviInfoControllerTest {

    @Autowired
    MovieInforepository inforepository;

    @Autowired
    WebTestClient webTestClient;

    private static String MOVIES_URL = "/v1/movieInfos";

    @BeforeEach
    void setUp() {

        var movieinfos = List.of(new MovieInfo(null, "Batman Begins",
                2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15")),
                new MovieInfo(null, "The Dark Knight",
                        2008, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18")),
                new MovieInfo("abc", "Dark Knight Rises",
                        2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20")));

        inforepository.saveAll(movieinfos)
                .blockLast();//pour être sur qu'il a tout enreg dans la bd
    }

    @AfterEach
    void tearDown() {
        inforepository.deleteAll().block();
    }
    
    @Test
    void testAddMovieInfo() {
        var newMovie = new MovieInfo(null, "Batman Begins 1",
        2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));
        webTestClient.post().uri(MOVIES_URL)
        .bodyValue(newMovie).exchange()
        .expectStatus().isCreated()
        .expectBody(MovieInfo.class)
        .consumeWith(m -> {
            var saved = m.getResponseBody();
            assertNotNull(saved.getMovieInfoId());
        });
    }

    @Test
    void testgetAllMoviesInfo() {
        webTestClient.get().uri(MOVIES_URL).exchange()
        .expectStatus().is2xxSuccessful()
        .expectBodyList(MovieInfo.class)
        .hasSize(3);
    } 

    @Test
    void testgetAllMoviesInfoByYear() {
        var uri = UriComponentsBuilder.fromUriString(MOVIES_URL)
        .queryParam("year", 2005).buildAndExpand().toUri();
        webTestClient.get().uri(uri).exchange()
        .expectStatus().is2xxSuccessful()
        .expectBodyList(MovieInfo.class)
        .hasSize(1);
    }

    @Test
    void testGeMoviebyId() {
        var id = "abc";
        webTestClient.get().uri(MOVIES_URL+"/{id}", id).exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody()
        .jsonPath("$.name").isEqualTo("Dark Knight Rises")
        /* .consumeWith(m -> {
            var saved = m.getResponseBody();
            assertNotNull(saved);
        })*/;
    }

    @Test
    void testUpdateMovieInfo() {
        var id = "abc";
        var newMovie = new MovieInfo(null, "Dark Knight Rises 1",
        2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));
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
        webTestClient.delete().uri(MOVIES_URL+"/{id}", id)
        .exchange()
        .expectStatus().isNoContent();
    }

    @Test
    void testUpdateMovieInfo_notfound() {
        var id = "def";
        var newMovie = new MovieInfo(null, "Dark Knight Rises 1",
        2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));
        webTestClient.put().uri(MOVIES_URL+"/{id}", id)
        .bodyValue(newMovie).exchange()
        .expectStatus().isNotFound();
    }

    @Test
    void testGeMoviebyId_notfound() {
        var id = "def";
        webTestClient.get().uri(MOVIES_URL+"/{id}", id).exchange()
        .expectStatus().isNotFound();
    }

    @Test
    void testgetAllMoviesInfo_stream() {
        //add d'abord
        var newMovie = new MovieInfo(null, "Batman Begins 1",
        2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));
        webTestClient.post().uri(MOVIES_URL)
        .bodyValue(newMovie).exchange()
        .expectStatus().isCreated()
        .expectBody(MovieInfo.class)
        .consumeWith(m -> {
            var saved = m.getResponseBody();
            assertNotNull(saved.getMovieInfoId());
        });

        var moviesStreamFlux = webTestClient.get().uri(MOVIES_URL+"/stream").exchange()
        .expectStatus().is2xxSuccessful()
        .returnResult(MovieInfo.class)
        .getResponseBody();
        StepVerifier.create(moviesStreamFlux).assertNext(m1 -> {
            assert m1.getMoviebyId() != null;
        }).thenCancel().verify();
    }
}
