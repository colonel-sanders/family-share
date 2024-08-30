package com.github.colonelsanders.familyshare.entities;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyMemberRepository extends CrudRepository<FamilyMember, Long> {
  FamilyMember findByName(String name);

  FamilyMember findByWebauthnHandle(byte[] handle);
}
