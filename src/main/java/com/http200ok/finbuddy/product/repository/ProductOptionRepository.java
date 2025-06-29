package com.http200ok.finbuddy.product.repository;

import com.http200ok.finbuddy.product.domain.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
}
