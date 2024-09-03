package com.github.colonelsanders.familyshare.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SecuredContentController.class)
class SecuredContentControllerTests {
  @Autowired private MockMvc mockMvc;

  @Test
  void rendersSecureHtml() throws Exception {
    this.mockMvc
        .perform(get("/secure"))
        .andExpectAll(
            status().isOk(),
            content().contentType("text/html"),
            content().string(containsString("<title>Family Share - Secured Content</title>")));
  }
}
