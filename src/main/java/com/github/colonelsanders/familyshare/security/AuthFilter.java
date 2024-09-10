package com.github.colonelsanders.familyshare.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthFilter implements Filter {
  private static final String USER_SESSION_KEY = "user";

  public static String getAuthenticatedUsername(HttpSession session) {
    return session == null ? null : (String) session.getAttribute(USER_SESSION_KEY);
  }

  public static void setAuthenticatedUsername(@NonNull String user, HttpSession session) {
    session.setAttribute(USER_SESSION_KEY, user);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
    var httpRequest = (HttpServletRequest) request;
    var session = httpRequest.getSession(false);
    if (httpRequest.getRequestURI().startsWith("/secure")
        && getAuthenticatedUsername(session) == null) {
      log.warn("Rejecting request: {}", httpRequest.getRequestURI());
      var httpResponse = (HttpServletResponse) response;
      httpResponse.sendError(HttpStatus.UNAUTHORIZED.value());
      return;
    }
    filterChain.doFilter(request, response);
  }
}
