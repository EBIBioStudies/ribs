package uk.ac.ebi.biostudies.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Created by ehsan on 16/03/2017. */
public class Session {
  private static final ThreadLocal<String> message = new ThreadLocal();

  public static void setUserMessage(String text) {
    message.set(text);
  }

  public static String getUserMessage() {
    return message.get();
  }

  public static void clearMessage() {
    message.remove();
  }

  public static String getUserName() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) return null;
    if (!(authentication instanceof AnonymousAuthenticationToken)) {
      String currentUserName = authentication.getName();
      return currentUserName;
    }
    return null;
  }

  public static User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) return null;
    if (authentication instanceof AnonymousAuthenticationToken) return null;
    return (User) authentication.getPrincipal();
  }
}
