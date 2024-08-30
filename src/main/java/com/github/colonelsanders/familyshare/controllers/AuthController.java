package com.github.colonelsanders.familyshare.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.colonelsanders.familyshare.entities.FamilyMember;
import com.github.colonelsanders.familyshare.entities.FamilyMemberRepository;
import com.github.colonelsanders.familyshare.entities.WebauthnAuthenticator;
import com.github.colonelsanders.familyshare.entities.WebauthnAuthenticatorRepository;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

/** Endpoints for Webauthn registration and login. */
@Controller
public class AuthController {
  @Autowired private RelyingParty relyingParty;
  @Autowired private FamilyMemberRepository userRepo;
  @Autowired private WebauthnAuthenticatorRepository authenticatorRepo;

  private final Log log = LogFactory.getLog(AuthController.class);

  public static final String USER_SESSION_KEY = "user";
  private static final String ASSERTION_SESSION_KEY = "assertion";
  private static final String REGISTRATION_SESSION_KEY = "registration";

  @GetMapping("/login/start")
  @ResponseBody
  public String loginStartGet(HttpSession session) {
    var assertionOpts = StartAssertionOptions.builder().build();
    try {
      var assertionReq = relyingParty.startAssertion(assertionOpts);
      session.setAttribute(ASSERTION_SESSION_KEY, assertionReq.toJson());
      return assertionReq.toCredentialsGetJson();
    } catch (JsonProcessingException e) {
      log.error("Webauthn login failure", e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webauthn login failure");
    }
  }

  @PostMapping("/login/finish")
  @ResponseBody
  public String loginFinishPost(@RequestBody String credentialJson, HttpSession session) {
    log.info(credentialJson);
    try {
      var assertionReq =
          AssertionRequest.fromJson((String) session.getAttribute(ASSERTION_SESSION_KEY));
      var credential = PublicKeyCredential.parseAssertionResponseJson(credentialJson);
      var assertionOpts =
          FinishAssertionOptions.builder().request(assertionReq).response(credential).build();
      var result = relyingParty.finishAssertion(assertionOpts);
      if (result.isSuccess()) {
        session.removeAttribute(ASSERTION_SESSION_KEY);
        session.setAttribute(USER_SESSION_KEY, result.getUsername());
        return "success";
      }
    } catch (AssertionFailedException | IOException e) {
      log.error("Webauthn login failure", e);
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webauthn login failure");
  }

  private void guardUserRegistration(FamilyMember user) {
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unrecognized user");
    }
    var existingAuth = authenticatorRepo.findAllByOwner(user);
    if (!existingAuth.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Already registered");
    }
  }

  @GetMapping("/register")
  public String registerGet(@RequestParam String name, Model model) {
    var user = userRepo.findByName(name);
    guardUserRegistration(user);
    model.addAttribute("user", user);
    return "register";
  }

  @PostMapping("/register/start")
  @ResponseBody
  public String registerStartPost(@RequestParam String name, HttpSession session) {
    var user = userRepo.findByName(name);
    guardUserRegistration(user);
    var identity =
        UserIdentity.builder()
            .name(user.getName())
            .displayName(user.getName())
            .id(new ByteArray(user.getWebauthnHandle()))
            .build();
    var regOpts = StartRegistrationOptions.builder().user(identity).build();
    try {
      var reg = relyingParty.startRegistration(regOpts);
      session.setAttribute(REGISTRATION_SESSION_KEY, reg.toJson());
      return reg.toCredentialsCreateJson();
    } catch (IOException e) {
      log.error("Webauthn registration failure", e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webauthn registration failure");
    }
  }

  @PostMapping("/register/finish")
  @ResponseBody
  public String registerFinishPost(@RequestBody String credentialJson, HttpSession session) {
    var regJson = (String) session.getAttribute(REGISTRATION_SESSION_KEY);
    if (regJson == null) {
      log.error("registerFinish request without registerStart session data");
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized");
    }
    try {
      var reg = PublicKeyCredentialCreationOptions.fromJson(regJson);
      var user = userRepo.findByName(reg.getUser().getName());
      var regResponse = PublicKeyCredential.parseRegistrationResponseJson(credentialJson);
      var finishOpts =
          FinishRegistrationOptions.builder().request(reg).response(regResponse).build();
      var result = relyingParty.finishRegistration(finishOpts);
      var authenticatorEntity =
          new WebauthnAuthenticator(
              user,
              null,
              result.getPublicKeyCose().getBytes(),
              result.getKeyId().getId().getBytes(),
              result.getSignatureCount());
      authenticatorRepo.save(authenticatorEntity);
      return "success";
    } catch (RegistrationFailedException | IOException e) {
      log.error("Webauthn registration failure", e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webauthn registration failure");
    }
  }
}
