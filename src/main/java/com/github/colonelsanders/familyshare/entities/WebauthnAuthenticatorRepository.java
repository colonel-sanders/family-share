package com.github.colonelsanders.familyshare.entities;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebauthnAuthenticatorRepository
    extends CrudRepository<WebauthnAuthenticator, Long> {
  List<WebauthnAuthenticator> findAllByOwner(FamilyMember owner);

  List<WebauthnAuthenticator> findAllByCredentialId(byte[] credentialId);
}
