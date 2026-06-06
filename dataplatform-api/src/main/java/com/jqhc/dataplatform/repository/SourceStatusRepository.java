package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.SourceStatusDoc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceStatusRepository extends JpaRepository<SourceStatusDoc, String> {
}
