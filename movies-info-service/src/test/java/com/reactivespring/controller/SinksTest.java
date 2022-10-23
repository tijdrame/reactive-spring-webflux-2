package com.reactivespring.controller;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

public class SinksTest {
    
    @Test
    void sink(){
        Sinks.Many<Integer> replaySink = Sinks.many().replay().all();
        replaySink.emitNext(1, EmitFailureHandler.FAIL_FAST);
        replaySink.emitNext(2, EmitFailureHandler.FAIL_FAST);

        Flux<Integer> integerFlux =replaySink.asFlux();
        integerFlux.subscribe(i -> {
            System.out.println("Subscriber 1: "+i);
        });

        Flux<Integer> integerFlux1 =replaySink.asFlux();
        integerFlux1.subscribe(i -> {
            System.out.println("Subscriber 2: "+i);
        });
        replaySink.tryEmitNext(3);
        //lui aussi recvra les 3 elements comme les 2 autres flux
        Flux<Integer> integerFlux2 =replaySink.asFlux();
        integerFlux2.subscribe(i -> {
            System.out.println("Subscriber 3: "+i);
        });
    }

    @Test
    void sink_multicast(){
        Sinks.Many<Integer> multicast = Sinks.many().multicast().onBackpressureBuffer();
        multicast.emitNext(1, EmitFailureHandler.FAIL_FAST);
        multicast.emitNext(2, EmitFailureHandler.FAIL_FAST);
        //recevra ts les elements
        Flux<Integer> integerFlux = multicast.asFlux();
        integerFlux.subscribe(i -> {
            System.out.println("Subscriber 1: "+i);
        });
        //ne recevra que les nouv elements (ici 3)
        Flux<Integer> integerFlux1 = multicast.asFlux();
        integerFlux1.subscribe(i -> {
            System.out.println("Subscriber 2: "+i);
        });
        multicast.emitNext(3, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    @Test
    void sink_unicast(){
        Sinks.Many<Integer> unicast = Sinks.many().unicast().onBackpressureBuffer();
        unicast.emitNext(1, EmitFailureHandler.FAIL_FAST);
        unicast.emitNext(2, EmitFailureHandler.FAIL_FAST);
        //recevra ts les elements
        Flux<Integer> integerFlux = unicast.asFlux();
        integerFlux.subscribe(i -> {
            System.out.println("Subscriber 1: "+i);
        }); 
        /* unicast permet un seul subscriber
        Flux<Integer> integerFlux1 = unicast.asFlux();
        integerFlux1.subscribe(i -> {
            System.out.println("Subscriber 2: "+i);
        });*/
        unicast.emitNext(3, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
