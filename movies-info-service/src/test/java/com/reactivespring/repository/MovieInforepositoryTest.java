package com.reactivespring.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import com.reactivespring.domain.MovieInfo;

import reactor.test.StepVerifier;

@DataMongoTest
@ActiveProfiles("test") // pour na pas use le
// profile par default avec la vrai bd mais
// la base embarquée mongo
public class MovieInforepositoryTest {

    @Autowired
    MovieInforepository inforepository;

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
    void testFindAll() {
        var moviesInfoFlux = inforepository.findAll().log();
        StepVerifier.create(moviesInfoFlux)
        .expectNextCount(3)
        .verifyComplete();
    }

    @Test
    void testFindById() {
        var moviesInfoMono = inforepository.findById("abc").log();
        StepVerifier.create(moviesInfoMono)
        .assertNext(m -> {
            assertEquals("Dark Knight Rises", m.getName());
        }).verifyComplete();
    }

    @Test
    void saveMovie() {
        var movieInfo = new MovieInfo(null, "Batman Begins",
        2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));
        var moviesInfoMono = inforepository.save(movieInfo).log();
        StepVerifier.create(moviesInfoMono)
        .assertNext(m -> {
            assertNotNull(m.getMovieInfoId());
            assertEquals("Batman Begins", m.getName());
        }).verifyComplete();
    }

    @Test
    void updateeMovie() {
        var movieInfo = inforepository.findById("abc").block();
        movieInfo.setYear(2021);
        var moviesInfoMono = inforepository.save(movieInfo).log();
        StepVerifier.create(moviesInfoMono)
        .assertNext(m -> {
            assertNotNull(m.getMovieInfoId());
            assertEquals("Dark Knight Rises", m.getName());
            assertEquals(2021, m.getYear());
            
        }).verifyComplete();
    }

    @Test
    void testDeleteMovie() {
        inforepository.deleteById("abc").block();
        var moviesInfoFlux = inforepository.findAll().log();
        StepVerifier.create(moviesInfoFlux)
        .expectNextCount(2)
        .verifyComplete();
    }

    @Test
    void testFindByYear(){
        var moviesInfoMono = inforepository.findByYear(2005);
        StepVerifier.create(moviesInfoMono)
        .assertNext(m -> {
            assertNotNull(m.getMovieInfoId());
            assertEquals("Batman Begins", m.getName());
            assertEquals(2005, m.getYear());
            
        }).verifyComplete();
    }

    @Test
    void testFindByName(){
        var moviesInfoMono = inforepository.findByName("The Dark Knight");
        StepVerifier.create(moviesInfoMono)
        .assertNext(m -> {
            assertNotNull(m.getMovieInfoId());
            assertEquals("The Dark Knight", m.getName());
            assertEquals(2008, m.getYear());
            
        }).verifyComplete();
    }
}
