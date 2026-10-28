package com.doulgs.testyourluck.field;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomFieldRepository extends JpaRepository<CustomField, UUID> {

    List<CustomField> findAllByOrderByCreatedAtAsc();

    boolean existsByFieldKey(String key);

    boolean existsByFieldKeyAndIdNot(String key, UUID id);
}
