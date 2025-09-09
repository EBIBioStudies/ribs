package uk.ac.ebi.biostudies.controller;


import static uk.ac.ebi.biostudies.api.util.HttpTools.sendRedirect;

import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biostudies.api.util.HttpTools;
import uk.ac.ebi.biostudies.auth.RestBasedAuthenticationProvider;
import uk.ac.ebi.biostudies.auth.Session;
import uk.ac.ebi.biostudies.auth.User;
import uk.ac.ebi.biostudies.auth.UserSecurityService;

/**
 * Created by ehsan on 15/03/2017.
 */

@RestController
public class Authentication {

    private Logger logger = LogManager.getLogger(Authentication.class.getName());


    @Autowired
    UserSecurityService users;
    @Autowired
    RestBasedAuthenticationProvider authenticationProvider;

    @RequestMapping(value = "/auth", method = RequestMethod.POST)
    public void login(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String returnURL = request.getHeader(HttpTools.REFERER_HEADER);
        String username = request.getParameter("u");
        String password = request.getParameter("p");
        String remember = request.getParameter("r");
        String requestURL = request.getRequestURL().toString();
        String applicationRoot = requestURL.substring(0, requestURL.indexOf(request.getServletPath()));

        if (returnURL != null && !returnURL.replaceFirst("https", "").replaceFirst("http", "").startsWith(applicationRoot.replaceFirst("https", "").replaceFirst("http", ""))) {
            returnURL = applicationRoot;
        }
        boolean isLoginSuccessful = false;
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
        org.springframework.security.core.Authentication userPassAuth = authenticationProvider.authenticate(authRequest);
        isLoginSuccessful = userPassAuth != null;
        // 31,557,600 is a standard year in seconds
        Integer maxAge = "on".equals(remember) ? HttpTools.MAX_AGE : null;

        if (isLoginSuccessful) {
            logger.info("Successfully authenticated user [{}]", username);
            HttpTools.setTokenCookie(response, ((User) userPassAuth.getPrincipal()).getToken(), maxAge);
        } else {
            String message = Session.getUserMessage();
            if (message != null && message.length() > 0) {
                HttpTools.setCookie(response, HttpTools.AUTH_MESSAGE_COOKIE, URLEncoder.encode(message, "UTF-8"), 5);
            } else {
                HttpTools.setCookie(response, HttpTools.AUTH_MESSAGE_COOKIE, URLEncoder.encode("Invalid username or password", "UTF-8"), 5);
            }
            Session.clearMessage();
        }

        sendRedirect(response, returnURL, isLoginSuccessful);
    }
}
