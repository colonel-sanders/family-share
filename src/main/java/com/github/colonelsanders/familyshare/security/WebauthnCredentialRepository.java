package com.github.colonelsanders.familyshare.security;

import com.github.colonelsanders.familyshare.entities.FamilyMemberRepository;
import com.github.colonelsanders.familyshare.entities.WebauthnAuthenticator;
import com.github.colonelsanders.familyshare.entities.WebauthnAuthenticatorRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * In this Webauthn implementation, the Credential Repository maps between authenticators and users.
 *
 * @see com.yubico.webauthn.CredentialRepository
 */
@Repository
public class WebauthnCredentialRepository implements CredentialRepository {
  @Autowired private FamilyMemberRepository familyMemberRepo;
  @Autowired private WebauthnAuthenticatorRepository authenticatorRepo;

  @Override
  public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
    var member = familyMemberRepo.findByName(username);
    var authenticators = authenticatorRepo.findAllByOwner(member);
    var descriptors = new HashSet<PublicKeyCredentialDescriptor>();
    for (var authenticator : authenticators) {
      descriptors.add(
          PublicKeyCredentialDescriptor.builder()
              .id(new ByteArray(authenticator.getCredentialId()))
              .build());
    }
    return descriptors;
  }

  @Override
  public Optional<ByteArray> getUserHandleForUsername(String username) {
    return Optional.of(new ByteArray(familyMemberRepo.findByName(username).getWebauthnHandle()));
  }

  @Override
  public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
    return Optional.of(familyMemberRepo.findByWebauthnHandle(userHandle.getBytes()).getName());
  }

  @Override
  public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
    var credentials = this.lookupAll(credentialId);
    return credentials.stream().findFirst();
  }

  @Override
  public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
    var authenticators = authenticatorRepo.findAllByCredentialId(credentialId.getBytes());
    return authenticators.stream()
        .map(WebauthnCredentialRepository::authenticatorToCredential)
        .collect(Collectors.toSet());
  }

  public static RegisteredCredential authenticatorToCredential(
      @NonNull WebauthnAuthenticator auth) {
    return RegisteredCredential.builder()
        .credentialId(new ByteArray(auth.getCredentialId()))
        .userHandle(new ByteArray(auth.getOwner().getWebauthnHandle()))
        .publicKeyCose(new ByteArray(auth.getPublicKeyCOSE()))
        .signatureCount(auth.getLastSigCount())
        .build();
  }
}
