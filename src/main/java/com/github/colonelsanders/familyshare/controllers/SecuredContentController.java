package com.github.colonelsanders.familyshare.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SecuredContentController {
  @GetMapping("/secure")
  public String index() {
    return "secure";
  }
}
