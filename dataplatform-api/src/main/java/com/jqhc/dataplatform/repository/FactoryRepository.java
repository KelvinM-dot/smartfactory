package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.FactoryDoc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactoryRepository extends JpaRepository<FactoryDoc, String> {
}
