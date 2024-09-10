package com.github.colonelsanders.familyshare.entities;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends ListCrudRepository<Document, Long> {
  List<Document> findAllByCategory(Category category);
}
