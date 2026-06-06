package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.RecipeDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<RecipeDoc, String> {

    List<RecipeDoc> findByProductLineId(String productLineId);
}
