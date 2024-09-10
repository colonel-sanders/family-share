package com.github.colonelsanders.familyshare.entities;

import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestDataFactory {
  @Autowired private FamilyMemberRepository userRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private DocumentRepository documentRepository;

  @EventListener(ApplicationReadyEvent.class)
  public void populateTestData() {
    log.info("Inserting test data...");
    userRepository.save(new FamilyMember(null, "Dad", HexFormat.of().parseHex("6443b19379b3a89f")));
    userRepository.save(new FamilyMember(null, "Mom", HexFormat.of().parseHex("8ca8e1d3ac97d896")));
    userRepository.save(
        new FamilyMember(null, "Bambino", HexFormat.of().parseHex("6f870e86c1b231c5")));

    var autosCategory = categoryRepository.save(new Category(null, "Automotive"));
    var homeCategory = categoryRepository.save(new Category(null, "Home"));

    documentRepository.save(
        new Document(
            autosCategory,
            null,
            "Insurance",
            """
            Reliable Insurance
            Policy 355-005441717
            Roadside Assistance: 800-999-5104
            Phone: 800-999-2334
            """));
    documentRepository.save(
        new Document(
            autosCategory,
            null,
            "Mom's Saab",
            """
            *2003 Saab 9-3*
            License: MI 233 AC4
            VIN: YS3FF46Y342144107
            """));
    documentRepository.save(
        new Document(
            homeCategory,
            null,
            "Security System",
            """
            **Alarm Code: 1111**
            Guardian Alarm Co
            800-222-9102
            ***Confirmation word: codswallop***
            """));
    log.info("Test data inserted");
  }
}
