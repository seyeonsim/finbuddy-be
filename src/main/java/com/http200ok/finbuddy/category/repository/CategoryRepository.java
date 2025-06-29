package com.http200ok.finbuddy.category.repository;

import com.http200ok.finbuddy.category.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
