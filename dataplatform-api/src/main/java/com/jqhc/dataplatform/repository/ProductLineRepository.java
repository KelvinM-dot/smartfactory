package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.ProductLineDoc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductLineRepository extends JpaRepository<ProductLineDoc, String> {
}
