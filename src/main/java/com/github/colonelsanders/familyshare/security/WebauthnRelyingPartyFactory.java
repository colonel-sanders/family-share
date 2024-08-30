package com.github.colonelsanders.familyshare.security;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * In this Webauthn implementation, the RelyingParty object encapsulates the server configuration
 * details.
 *
 * @see com.yubico.webauthn.RelyingParty
 */
@Component
public class WebauthnRelyingPartyFactory {

  @Bean
  public RelyingParty relyingParty(
      @Autowired WebauthnCredentialRepository credentialRepo,
      @Value("${webauthn.display-name}") String displayName,
      @Value("${webauthn.hostname}") String hostname,
      @Value("${webauthn.origin-url}") String origin) {
    var identity = RelyingPartyIdentity.builder().id(hostname).name(displayName).build();
    return RelyingParty.builder()
        .identity(identity)
        .credentialRepository(credentialRepo)
        .origins(Collections.singleton(origin))
        .build();
  }
}
