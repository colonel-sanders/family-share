package com.github.colonelsanders.familyshare.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The user entity in the system. */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class FamilyMember {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Lob
  @Column(length = 64)
  private byte[] webauthnHandle;
}
