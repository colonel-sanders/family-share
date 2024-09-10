package com.github.colonelsanders.familyshare.entities;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends ListCrudRepository<Category, Long> {
  Category findByName(String name);
}
