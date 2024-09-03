package com.github.colonelsanders.familyshare.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.colonelsanders.familyshare.entities.FamilyMember;
import com.github.colonelsanders.familyshare.entities.FamilyMemberRepository;
import com.github.colonelsanders.familyshare.entities.WebauthnAuthenticatorRepository;
import com.github.colonelsanders.familyshare.security.WebauthnCredentialRepository;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
class AuthControllerTests {
  @Autowired private MockMvc mockMvc;
  @MockBean private RelyingParty relyingParty;
  @MockBean private FamilyMemberRepository mockUserRepo;
  @MockBean private WebauthnAuthenticatorRepository mockAuthenticatorRepo;
  @MockBean private WebauthnCredentialRepository mockCredentialRepo;

  @Nested
  public class LoginTests {
    private static final String SAMPLE_ASSERTION_JSON =
        "{\"publicKeyCredentialRequestOptions\":{\"challenge\":\"590Ye1oSwsHeKuS6WkfDotDiJEYBRCMfOSLo-2HS7TQ\",\"rpId\":\"localhost\",\"extensions\":{}}}";
    private static final String SAMPLE_CREDENTIAL_GET_JSON =
        "{ \"publicKey\": { \"rpId\": \"localhost\", \"extensions\": {} } }";
    private static final String SAMPLE_CREDENTIAL_RESPONSE_JSON =
        """
                {
                  "id": "ztOvKlzZroPggXYPBNi0eBwhc6M",
                  "type": "public-key",
                  "clientExtensionResults": {},
                  "response": {
                     "authenticatorData": "SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2MdAAAAAA",
                     "signature": "MEUCIQCrPgTfI6t2VD1cnfqcOnRnLlOHxcjMqSDZN0Rxf4_9WAIgMmj4fdjK7Jt-00tG-nCs4lRFfn1taea_y_sD0Fdu1bI",
                     "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiaVo4OXNZcXVtSTg1VHdmX0FKY181RERRSUVyUF9DY1FQQ2JONWc4SmYtMCIsIm9yaWdpbiI6Imh0dHBzOi8vbG9jYWxob3N0OjgwODAiLCJjcm9zc09yaWdpbiI6ZmFsc2V9",
                     "userHandle":"NjQ0M2IxOTM3OWIzYTg5Zg"
                  }
                }""";

    @BeforeEach
    void loginMocks() throws Exception {
      var mockAssertionReq = mock(AssertionRequest.class);
      when(relyingParty.startAssertion(any(StartAssertionOptions.class)))
          .thenReturn(mockAssertionReq);
      when(mockAssertionReq.toJson()).thenReturn(SAMPLE_ASSERTION_JSON);
      when(mockAssertionReq.toCredentialsGetJson()).thenReturn(SAMPLE_CREDENTIAL_GET_JSON);
    }

    @Test
    void loginStartGeneratesAssertionRequest() throws Exception {
      mockMvc
          .perform(get("/login/start"))
          .andExpectAll(
              status().isOk(),
              content().contentType("application/json"),
              content().json(SAMPLE_CREDENTIAL_GET_JSON));
    }

    @Test
    void loginFinishReturns401IfNotStarted() throws Exception {
      mockMvc
          .perform(
              post("/login/finish")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(SAMPLE_CREDENTIAL_RESPONSE_JSON))
          .andExpect(status().is(401));
    }

    @Test
    void loginFinishFailsWithBadPayloadJSON() throws Exception {
      mockMvc
          .perform(
              post("/login/finish")
                  .sessionAttr(AuthController.ASSERTION_SESSION_KEY, SAMPLE_ASSERTION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{ \"bad\": \"payload\" }"))
          .andExpect(status().is4xxClientError());
    }

    @Test
    void loginFinishRejectsWhenAuthFails() throws Exception {
      when(relyingParty.finishAssertion(any(FinishAssertionOptions.class)))
          .thenThrow(new AssertionFailedException("Test Exception"));
      mockMvc
          .perform(
              post("/login/finish")
                  .sessionAttr(AuthController.ASSERTION_SESSION_KEY, SAMPLE_ASSERTION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(SAMPLE_CREDENTIAL_RESPONSE_JSON))
          .andExpect(status().is4xxClientError());
    }

    @Test
    void loginFinishReturnsSuccess() throws Exception {
      var mockResult = mock(AssertionResult.class);
      when(mockResult.isSuccess()).thenReturn(true);
      when(mockResult.getUsername()).thenReturn("TestUser");
      when(relyingParty.finishAssertion(any(FinishAssertionOptions.class))).thenReturn(mockResult);
      mockMvc
          .perform(
              post("/login/finish")
                  .sessionAttr(AuthController.ASSERTION_SESSION_KEY, SAMPLE_ASSERTION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(SAMPLE_CREDENTIAL_RESPONSE_JSON))
          .andExpectAll(
              status().isOk(),
              content().contentType("application/json"),
              content().json("{ \"status\": \"success\", \"username\": \"TestUser\" }"));
    }
  }

  @Nested
  public class RegistrationTests {
    private static final String SAMPLE_CREATION_JSON =
        "{\"rp\":{\"name\":\"Family Share\",\"id\":\"localhost\"},\"user\":{\"name\":\"TestUser\",\"displayName\":\"TestUser\",\"id\":\"NjQ0M2IxOTM3OWIzYTg5Zg\"},\"challenge\":\"634aeTfkZYWPezh-OaYcZ5WF0ofkg9lRUqUVLCMGCiA\",\"pubKeyCredParams\":[{\"alg\":-7,\"type\":\"public-key\"},{\"alg\":-8,\"type\":\"public-key\"},{\"alg\":-35,\"type\":\"public-key\"},{\"alg\":-36,\"type\":\"public-key\"},{\"alg\":-257,\"type\":\"public-key\"},{\"alg\":-258,\"type\":\"public-key\"},{\"alg\":-259,\"type\":\"public-key\"}],\"excludeCredentials\":[],\"attestation\":\"none\",\"extensions\":{\"credProps\":true}}";
    private static final String SAMPLE_CREDENTIAL_CREATE_JSON =
        "{\"publicKey\":" + SAMPLE_CREATION_JSON + "}";
    private static final String SAMPLE_CREATE_RESPONSE_JSON =
        """
                   {
                     "id": "aGBIrQcPUpC-lMcVFxU0_w0wkTY",
                     "type": "public-key",
                     "clientExtensionResults": {"credProps":{"rk":true}},
                     "response": {
                         "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoiVUtBYWpoX2JQckpDTlZnNW11SEpiVXR3V2JvZlBaZ2pfZ21qWVlYcjlhQSIsIm9yaWdpbiI6Imh0dHBzOi8vbG9jYWxob3N0OjgwODAiLCJjcm9zc09yaWdpbiI6ZmFsc2V9",
                         "attestationObject":"o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YViYSZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2NdAAAAAPv8MAcVTk7MjAtuAgVX170AFGhgSK0HD1KQvpTHFRcVNP8NMJE2pQECAyYgASFYIHb1Uu7BqkQUQ3Wb0d1bxNJrht0VS_K4K_bOrDTokM7KIlgg_-7HMhdJjgsul8DJDYBU4a8ox0MX7OM2cWj-NeYfLSk"
                     }
                   }""";

    @BeforeEach
    void registrationMocks() throws Exception {
      when(mockUserRepo.findByName(anyString()))
          .thenReturn(new FamilyMember(123L, "TestUser", new byte[] {7, 4, 2, 2, 1}));
      when(mockAuthenticatorRepo.findAllByOwner(any(FamilyMember.class))).thenReturn(List.of());
      var mockCreateOpts = mock(PublicKeyCredentialCreationOptions.class);
      when(relyingParty.startRegistration(any(StartRegistrationOptions.class)))
          .thenReturn(mockCreateOpts);
      when(mockCreateOpts.toJson()).thenReturn(SAMPLE_CREATION_JSON);
      when(mockCreateOpts.toCredentialsCreateJson()).thenReturn(SAMPLE_CREDENTIAL_CREATE_JSON);
      var mockRegistrationResult = mock(RegistrationResult.class);
      when(relyingParty.finishRegistration(any(FinishRegistrationOptions.class)))
          .thenReturn(mockRegistrationResult);
      when(mockRegistrationResult.getPublicKeyCose()).thenReturn(ByteArray.fromHex("a14bb2"));
      when(mockRegistrationResult.getKeyId())
          .thenReturn(
              PublicKeyCredentialDescriptor.builder().id(ByteArray.fromHex("b14cc3")).build());
      when(mockRegistrationResult.getSignatureCount()).thenReturn(0L);
    }

    @Test
    void registerRequiresUsername() throws Exception {
      mockMvc.perform(get("/register")).andExpect(status().isBadRequest());
    }

    @Test
    void registerProducesHTML() throws Exception {
      mockMvc
          .perform(get("/register").param("name", "TestUser"))
          .andExpectAll(
              status().isOk(),
              content().contentType("text/html"),
              content().string(containsString("<title>Family Share - Finish Registration</title>")),
              content().string(containsString("src=\"/js/login.js\"")));
    }

    @Test
    void registerStartGeneratesAssertionRequest() throws Exception {
      mockMvc
          .perform(post("/register/start").param("name", "TestUser"))
          .andExpectAll(
              status().isOk(),
              content().contentType("application/json"),
              content()
                  .json(
                      "{ \"publicKey\": { \"rp\":{\"name\":\"Family Share\",\"id\":\"localhost\"},\"user\":{\"name\":\"TestUser\",\"displayName\":\"TestUser\",\"id\":\"NjQ0M2IxOTM3OWIzYTg5Zg\" }} }"));
    }

    @Test
    void registerFinishReturns401IfNotStarted() throws Exception {
      mockMvc
          .perform(
              post("/register/finish")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(SAMPLE_CREATE_RESPONSE_JSON))
          .andExpect(status().is(401));
    }

    @Test
    void registerFinishFailsWithBadPayloadJSON() throws Exception {
      mockMvc
          .perform(
              post("/register/finish")
                  .sessionAttr(AuthController.REGISTRATION_SESSION_KEY, SAMPLE_CREATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{ \"bad\": \"payload\" }"))
          .andExpect(status().is4xxClientError());
    }

    @Test
    void registerFinishRejectsWhenAuthFails() throws Exception {
      when(relyingParty.finishRegistration(any(FinishRegistrationOptions.class)))
          .thenThrow(
              new RegistrationFailedException(new IllegalArgumentException("Test Exception")));
      mockMvc
          .perform(
              post("/register/finish")
                  .sessionAttr(AuthController.REGISTRATION_SESSION_KEY, SAMPLE_CREATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(SAMPLE_CREATE_RESPONSE_JSON))
          .andExpect(status().is4xxClientError());
    }

    @Test
    void registerFinishSucceeds() throws Exception {
      mockMvc
          .perform(
              post("/register/finish")
                  .sessionAttr(AuthController.REGISTRATION_SESSION_KEY, SAMPLE_CREATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(SAMPLE_CREATE_RESPONSE_JSON))
          .andExpectAll(
              status().isOk(),
              content().contentType("application/json"),
              content().json("{ \"status\": \"success\", \"username\": \"TestUser\" }"));
    }
  }
}
