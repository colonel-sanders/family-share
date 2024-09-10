package com.github.colonelsanders.familyshare.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.colonelsanders.familyshare.entities.CategoryRepository;
import com.github.colonelsanders.familyshare.entities.DocumentRepository;
import com.github.colonelsanders.familyshare.security.AuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SecuredContentController.class)
class SecuredContentControllerTests {
  @Autowired private MockMvc mockMvc;
  @MockBean private CategoryRepository mockCategoryRepo;
  @MockBean private DocumentRepository mockDocumentRepo;

  @Test
  void requiresAuth() throws Exception {
    this.mockMvc.perform(get("/secure")).andExpect(status().isUnauthorized());
  }

  @Test
  void rendersSecureHtml() throws Exception {
    this.mockMvc
        .perform(get("/secure/").sessionAttr(AuthFilter.USER_SESSION_KEY, "Dad"))
        .andExpectAll(
            status().isOk(),
            content().contentType("text/html"),
            content().string(containsString("<title>Family Share - Secured Content</title>")));
  }
}
