package uk.ac.ebi.biostudies.auth;

import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

public class RefererAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
  public RefererAuthenticationSuccessHandler() {
    super();
    setUseReferer(true);
  }
}
