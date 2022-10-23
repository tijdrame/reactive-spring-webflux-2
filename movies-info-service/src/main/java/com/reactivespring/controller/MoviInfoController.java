package com.reactivespring.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.service.MovieInfoService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/movieInfos")
public class MoviInfoController {

    private final MovieInfoService service;
    Sinks.Many<MovieInfo> movieInfoSink = Sinks.many().replay().all();

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MovieInfo> addMovieInfo(@Valid @RequestBody MovieInfo movieInfo) {
        return service.addMovieInfo(movieInfo)
                .doOnNext(savedinfo -> movieInfoSink.tryEmitNext(savedinfo));
    }

    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Flux<MovieInfo> getStream() {
        return movieInfoSink.asFlux().log();
    }

    @GetMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Flux<MovieInfo> getAllMovies(@RequestParam(value = "year", required = false) Integer year) {
        if (year != null) {
            return service.findByYear(year);
        }
        return service.getAll();
    }

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ResponseEntity<MovieInfo>> getMoviebyId(@PathVariable String id) {
        return service.getById(id).map(ResponseEntity.accepted()::body)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ResponseEntity<MovieInfo>> updateMoviebyId(@PathVariable String id,
            @Valid @RequestBody MovieInfo movieInfo) {
        return service.updateMovie(id, movieInfo)
                .map(ResponseEntity.ok()::body)// transform Mono<MovieInfo> en Mono<ResponseEntity< MovieInfo>>
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));// ce qu'on retourne quand on ne trouve
                                                                             // rien
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMoviebyId(@PathVariable String id) {
        return service.deleteMoviebyId(id);
    }

}
