package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.ProductSpecDoc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSpecRepository extends JpaRepository<ProductSpecDoc, String> {
}
