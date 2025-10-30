package uk.ac.ebi.biostudies.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller to handle custom URL redirections outside the main application context path.
 */
@Component
public class RedirectController {

  /**
   * Handles requests to /bioairepo and redirects them to /biostudies/BioAIrepo.
   * <p>
   * Since the application context path is set to /biostudies, incoming requests for /bioairepo
   * need to be redirected to the appropriate path within the app.
   * </p>
   *
   * @return a RedirectView that issues a 302 redirect to /biostudies/BioAIrepo
   */
  @GetMapping("/bioairepo")
  public RedirectView redirectBioAIRepo() {
    return new RedirectView("/biostudies/BioAIrepo");
  }
}
