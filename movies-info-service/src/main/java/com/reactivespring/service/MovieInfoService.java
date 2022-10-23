package com.reactivespring.service;

import org.springframework.stereotype.Service;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.repository.MovieInforepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class MovieInfoService {

    private final MovieInforepository repository;

    public Mono<MovieInfo> addMovieInfo(MovieInfo movieInfo) {
        return repository.save(movieInfo);
    }

    public Flux<MovieInfo> getAll() {
        return repository.findAll();
    }

    public Mono<MovieInfo> getById(String id) {
        return repository.findById(id);
    }

    public Mono<MovieInfo> updateMovie(String id, MovieInfo movieInfo) {
        return repository.findById(id).flatMap(m -> {
            m.setCast(movieInfo.getCast());
            m.setName(movieInfo.getName());
            m.setReleaseDate(movieInfo.getReleaseDate());
            m.setYear(movieInfo.getYear());
            return repository.save(m);
        });
    }

    public Mono<Void> deleteMoviebyId(String id) {
        return repository.deleteById(id);
    }

    public Flux<MovieInfo> findByYear(Integer year) {
        return repository.findByYear(year);
    }
    
    
}
