package com.http200ok.finbuddy.product.repository;

import com.http200ok.finbuddy.product.domain.CheckingProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckingProductRepository extends JpaRepository<CheckingProduct, Long> {
}
