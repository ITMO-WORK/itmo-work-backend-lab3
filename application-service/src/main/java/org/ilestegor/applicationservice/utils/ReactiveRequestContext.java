package org.ilestegor.applicationservice.utils;

import reactor.core.publisher.Mono;

public class ReactiveRequestContext {

    public static Mono<String> getAuthTokenFromContext(){
        return Mono.deferContextual(context -> {
            if (context.hasKey("authToken")){
                return Mono.just(context.get("authToken"));
            }
            return Mono.empty();
        });
    }
}
