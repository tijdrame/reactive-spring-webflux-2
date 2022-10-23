package com.reactivespring.domain;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class MovieInfo {

    @Id
    private String movieInfoId;

    @NotBlank(message = "Name must be present")
    private String name;

    @NotNull
    @Positive(message = "Year must be a positive value")
    private Integer year;

    private List<@NotBlank(message = "Cast must be present")String> cast;

    private LocalDate releaseDate;
}
