package uk.ac.ebi.biostudies.auth;

import java.util.Arrays;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.ac.ebi.biostudies.api.util.HttpTools;

@Service
public class CustomRememberMeCookieService implements RememberMeServices {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    UserSecurityService userSecurityService;

    @Override
    public Authentication autoLogin(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String rememberMeCookie = this.extractRememberMeCookie(httpServletRequest);
        if (rememberMeCookie == null) return null;
        if (rememberMeCookie.length() != 0) {
            try {
                User user = userSecurityService.checkAccess(rememberMeCookie);
                if (user == null || user.getAllow() == null) return null;
                logger.debug("Remember-me cookie accepted");
                return this.createSuccessfulAuthentication(httpServletRequest, user);
            } catch (CookieTheftException exp) {
                this.cancelCookie(httpServletRequest, httpServletResponse);
                throw exp;
            } catch (Throwable exp) {
                logger.debug(exp.getMessage());
            }
        }
        cancelCookie(httpServletRequest, httpServletResponse);
        return null;
    }

    @Override
    public void loginFail(HttpServletRequest request, HttpServletResponse response) {
        onLoginFail(request, response);
    }

    protected void onLoginFail(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("Interactive login attempt was unsuccessful.");
    }

    @Override
    public void loginSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        String parameter = "r";
        if (!rememberMeRequested(httpServletRequest, parameter)) {
            logger.debug("Remember-me login not requested.");
        } else {
            onLoginSuccess(httpServletRequest, httpServletResponse, authentication);
        }
    }

    protected boolean rememberMeRequested(HttpServletRequest request, String parameter) {
        String paramValue = request.getParameter(parameter);
        if (paramValue == null) return false;
        return paramValue.equalsIgnoreCase("true")
                || paramValue.equalsIgnoreCase("on")
                || paramValue.equalsIgnoreCase("yes")
                || paramValue.equals("1");
    }

    public void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
        String token = ((User) successfulAuthentication.getPrincipal()).getToken();
        if (!StringUtils.hasLength(token)) {
            logger.debug("Unable to retrieve token for cookie");
        }
        HttpTools.setTokenCookie(response, token, HttpTools.MAX_AGE);
    }

    protected Authentication createSuccessfulAuthentication(HttpServletRequest request, User user) {
        return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    }

    protected String extractRememberMeCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        return cookies == null ? null :
                Arrays.stream(cookies)
                        .filter(cookie -> HttpTools.TOKEN_COOKIE.equals(cookie.getName()))
                        .findAny()
                        .map(Cookie::getValue)
                        .orElse(null);
    }

    protected void cancelCookie(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("Cancelling cookie");
        HttpTools.removeTokenCookie(response);
    }
}