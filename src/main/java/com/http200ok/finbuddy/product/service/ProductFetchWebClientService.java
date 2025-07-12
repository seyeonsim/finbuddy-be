package com.http200ok.finbuddy.product.service;

import reactor.core.publisher.Mono;

public interface ProductFetchWebClientService {
    Mono<Void> fetchAndSaveProducts(String productType);
}
