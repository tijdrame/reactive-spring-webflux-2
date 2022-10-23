package com.reactivespring.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.reactivespring.handler.ReviewHandler;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

@Configuration
public class ReviewRouter {

    @Bean
    public RouterFunction<ServerResponse> reviewsRoute(ReviewHandler handler) {
        return route()
        .nest(path("/v1/reviews"), builder -> {
            builder.POST("", request -> handler.addReview(request))
                .GET("", request -> handler.getReviews(request))
                .PUT("/{id}", request -> handler.updateReview(request))
                .DELETE("/{id}", request -> handler.deleteReview(request))
                .GET("/stream", request -> handler.getReviewStream(request));
        })
        .GET("/v1/helloworld",
        //handling basic sous forme de lambdas
                (request -> ServerResponse.ok().bodyValue("Hello world!!")))
        //.POST("/v1/reviews", request -> handler.addReview(request))
        //.GET("/v1/reviews", request -> handler.getReviews())
                .build();
    }
}
