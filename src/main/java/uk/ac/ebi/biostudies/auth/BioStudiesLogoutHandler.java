package uk.ac.ebi.biostudies.auth;

import static uk.ac.ebi.biostudies.api.util.HttpTools.sendRedirect;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.HttpTools;

@Service
public class BioStudiesLogoutHandler implements LogoutHandler {
    private final static Logger LOGGER = LogManager.getLogger(LogoutHandler.class.getName());
    @Autowired
    UserSecurityService users;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            User user = Session.getCurrentUser();
            if (user != null) {
                LOGGER.info("Logging out user [{}]", user.getLogin());
                users.logout();
            }
            HttpTools.removeTokenCookie(response);
            HttpSession session = request.getSession(false);
            if (session != null) {
                LOGGER.debug("Invalidating session: " + session.getId());
                session.invalidate();
            }
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(null);
            SecurityContextHolder.clearContext();
            String returnURL = request.getHeader(HttpTools.REFERER_HEADER);
            sendRedirect(response, returnURL, true);
        } catch (Exception ex) {
            LOGGER.error("logout exception", ex);
        }
    }

}
