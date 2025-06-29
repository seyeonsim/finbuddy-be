package com.http200ok.finbuddy.product.controller;

import com.http200ok.finbuddy.common.dto.PagedResponseDto;
import com.http200ok.finbuddy.product.dto.*;
import com.http200ok.finbuddy.product.service.ProductFetchService;
import com.http200ok.finbuddy.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductFetchService productFetchService;
    private final ProductService productService;

    /**
     * 예금 데이터 수집 및 저장
     */
    @PostMapping("/deposits/fetch")
    public ResponseEntity<String> fetchDepositData() {
        productFetchService.fetchAndSaveProducts("deposit");
        return ResponseEntity.ok("예금 데이터 저장 완료!");
    }

    /**
     * 적금 데이터 수집 및 저장
     */
    @PostMapping("/savings/fetch")
    public ResponseEntity<String> fetchSavingData() {
        productFetchService.fetchAndSaveProducts("saving");
        return ResponseEntity.ok("적금 데이터 저장 완료!");
    }

    /**
     * 예금 상품 목록 조회 (페이징 처리) - 최신순
     * 상품명(name), 은행명(bankName) 검색
     */
    @GetMapping("/deposits")
    public ResponseEntity<PagedResponseDto<ProductDto>> getDepositProducts(
            @RequestParam(value = "name", required = false, defaultValue = "") String name,
            @RequestParam(value = "bankName", required = false, defaultValue = "") String bankName,
            @RequestParam(value = "page", defaultValue = "0") int page) {

        PagedResponseDto<ProductDto> depositProducts = productService.searchDepositProductsByNameAndBank(name, bankName, page);

        return ResponseEntity.ok(depositProducts);
    }

    /**
     * 예금 상품 ID로 상세 조회
     */
    @GetMapping("/deposit/{productId}")
    public ResponseEntity<DepositProductDto> getDepositProductById(@PathVariable("productId") Long productId) {
        DepositProductDto depositProduct = productService.getDepositProductById(productId);
        return ResponseEntity.ok(depositProduct);
    }

    /**
     * 적금 상품 목록 조회 (페이징 처리) - 최신순
     * 상품명(name), 은행명(bankName) 검색
     */
    @GetMapping("/savings")
    public ResponseEntity<PagedResponseDto<ProductDto>> getSavingProducts(
            @RequestParam(value = "name", required = false, defaultValue = "") String name,
            @RequestParam(value = "bankName", required = false, defaultValue = "") String bankName,
            @RequestParam(value = "page", defaultValue = "0") int page) {

        PagedResponseDto<ProductDto> savingProducts =
                productService.searchSavingProductsByNameAndBank(name, bankName, page);

        return ResponseEntity.ok(savingProducts);
    }

    /**
     * 적금 상품 ID로 상세 조회
     */
    @GetMapping("/saving/{productId}")
    public ResponseEntity<SavingProductDto> getSavingProductById(@PathVariable("productId") Long productId) {
        SavingProductDto savingProduct = productService.getSavingProductById(productId);
        return ResponseEntity.ok(savingProduct);
    }

    /**
     * 추천 상품 조회
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendedProductDto>> getRecommendedProducts() {
        List<RecommendedProductDto> response = productService.getTopRecommendedProducts();
        return ResponseEntity.ok(response);
    }
}
