package com.github.colonelsanders.familyshare.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Describes a Webauthn authenticator device owned by a user. */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class WebauthnAuthenticator {

  @ManyToOne private FamilyMember owner;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Lob
  @Column(nullable = false)
  private byte[] publicKeyCOSE;

  @Lob
  @Column(nullable = false)
  private byte[] credentialId;

  private Long lastSigCount;
}
