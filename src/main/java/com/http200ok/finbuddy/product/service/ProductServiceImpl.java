package com.http200ok.finbuddy.product.service;

import com.http200ok.finbuddy.common.dto.PagedResponseDto;
import com.http200ok.finbuddy.product.domain.DepositProduct;
import com.http200ok.finbuddy.product.domain.Product;
import com.http200ok.finbuddy.product.domain.ProductOption;
import com.http200ok.finbuddy.product.domain.SavingProduct;
import com.http200ok.finbuddy.product.dto.*;
import com.http200ok.finbuddy.product.repository.DepositProductRepository;
import com.http200ok.finbuddy.product.repository.SavingProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final DepositProductRepository depositProductRepository;
    private final SavingProductRepository savingProductRepository;

    @Override
    public PagedResponseDto<ProductDto> searchDepositProductsByNameAndBank(
            String name, String bankName, int page) {

        PageRequest pageable = PageRequest.of(page, 5, Sort.by(Sort.Order.desc("disclosureStartDate")));

        Page<DepositProduct> products = depositProductRepository
                .findByNameContainingAndBank_NameContainingOrderByDisclosureStartDateDesc(name, bankName, pageable);

        // Product → ProductDto 변환하여 페이징 응답 생성
        Page<ProductDto> dtoPage = products.map(ProductDto::new);
        return new PagedResponseDto<>(dtoPage);
    }

    @Override
    public PagedResponseDto<ProductDto> searchSavingProductsByNameAndBank(
            String name, String bankName, int page) {

        PageRequest pageable = PageRequest.of(page, 5, Sort.by(Sort.Order.desc("disclosureStartDate")));

        Page<SavingProduct> products = savingProductRepository
                .findByNameContainingAndBank_NameContainingOrderByDisclosureStartDateDesc(name, bankName, pageable);

        // Product → ProductDto 변환하여 페이징 응답 생성
        Page<ProductDto> dtoPage = products.map(ProductDto::new);
        return new PagedResponseDto<>(dtoPage);
    }

    @Override
    public DepositProductDto getDepositProductById(Long productId) {
        DepositProduct depositProduct = depositProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));
        return new DepositProductDto(depositProduct);
    }

    @Override
    public SavingProductDto getSavingProductById(Long productId) {
        SavingProduct savingProduct = savingProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("적금 상품을 찾을 수 없습니다. ID: " + productId));
        return new SavingProductDto(savingProduct);
    }

    @Override
    public List<RecommendedProductDto> getTopRecommendedProducts() {
        // 예금 3개 조회
        List<DepositProduct> depositProducts = depositProductRepository.findTop3DepositProductsByMaxInterestRate(PageRequest.of(0, 3));
        List<RecommendedProductDto> depositDtos = depositProducts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // 적금 3개 조회
        List<SavingProduct> savingProducts = savingProductRepository.findTop3SavingProductsByMaxInterestRate(PageRequest.of(0, 3));
        List<RecommendedProductDto> savingDtos = savingProducts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // 예금-적금-예금-적금-예금-적금 순서로 정렬
        return interleaveLists(depositDtos, savingDtos);
    }

    private RecommendedProductDto convertToDto(Product product) {
        List<? extends ProductOption> options = product instanceof DepositProduct
                ? ((DepositProduct) product).getOptions()
                : ((SavingProduct) product).getOptions();

        Double minInterestRate = options.stream()
                .mapToDouble(ProductOption::getInterestRate)
                .min().orElse(0.0);

        Double maxInterestRate = options.stream()
                .mapToDouble(ProductOption::getMaximumInterestRate)
                .max().orElse(0.0);

        Integer minSavingTerm = options.stream()
                .mapToInt(ProductOption::getSavingTerm)
                .min().orElse(0);

        Integer maxSavingTerm = options.stream()
                .mapToInt(ProductOption::getSavingTerm)
                .max().orElse(0);

        String interestRateTypeName = options.get(0).getInterestRateTypeName(); // 동일하다고 가정

        return new RecommendedProductDto(
                product.getId(),
                product instanceof DepositProduct ? "DEPOSIT" : "SAVING",
                product.getName(),
                product.getBank().getId(),
                product.getBank().getName(),
                product.getBank().getLogoUrl(),
                interestRateTypeName,
                minInterestRate,
                maxInterestRate,
                minSavingTerm,
                maxSavingTerm
        );
    }

    private List<RecommendedProductDto> interleaveLists(List<RecommendedProductDto> list1, List<RecommendedProductDto> list2) {
        List<RecommendedProductDto> result = new ArrayList<>();
        int maxSize = Math.max(list1.size(), list2.size());

        for (int i = 0; i < maxSize; i++) {
            if (i < list1.size()) {
                result.add(list1.get(i)); // 예금
            }
            if (i < list2.size()) {
                result.add(list2.get(i)); // 적금
            }
        }

        return result;
    }
}
