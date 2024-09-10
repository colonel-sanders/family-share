package com.github.colonelsanders.familyshare.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class Document {
  @ManyToOne private Category category;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  @Audited
  private String name;

  @Audited private String content;
}
