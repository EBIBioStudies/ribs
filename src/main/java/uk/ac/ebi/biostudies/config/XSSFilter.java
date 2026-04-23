package uk.ac.ebi.biostudies.config;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Rejects obviously malicious request targets (URI + query string),
 * but does not alter request data.
 *
 * Real XSS protection must be done with context-aware output encoding
 * at the rendering layer.
 */
@Component
public class XSSFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(XSSFilter.class);

    private static final int MAX_INSPECTION_LENGTH = 8192;

    private static final Pattern[] SUSPICIOUS_PATTERNS = new Pattern[] {
        Pattern.compile("(?i)<\\s*/?\\s*(script|iframe|object|embed|link|meta|style|base|svg|math)\\b"),
        Pattern.compile("(?i)\\bon[a-z]{2,}\\s*="),
        Pattern.compile("(?i)\\b(?:javascript|vbscript)\\s*:"),
        Pattern.compile("(?i)\\beval\\s*\\("),
        Pattern.compile("(?i)\\bexpression\\s*\\("),
        Pattern.compile("(?i)<\\s*img\\b"),
        Pattern.compile("(?i)<\\s*svg\\b"),
        Pattern.compile("(?i)<\\s*math\\b")
    };

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\u0000-\\u001F\\u007F]");
    private static final Pattern CRLF = Pattern.compile("[\\r\\n]");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("XssProtectionFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestTarget = buildRequestTarget(httpRequest);

        if (isSuspicious(requestTarget)) {
            log.warn("Blocked suspicious request target: {}", safeForLog(requestTarget));
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.info("XssProtectionFilter destroyed");
    }

    private String buildRequestTarget(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        if (query == null || query.isEmpty()) {
            return uri;
        }
        return uri + "?" + query;
    }

    private boolean isSuspicious(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        String inspected = value.length() > MAX_INSPECTION_LENGTH
            ? value.substring(0, MAX_INSPECTION_LENGTH)
            : value;

        if (CONTROL_CHARS.matcher(inspected).find() || CRLF.matcher(inspected).find()) {
            return true;
        }

        if (matchesAny(inspected)) {
            return true;
        }

        String decodedOnce = urlDecodeSafely(inspected);
        return !decodedOnce.equals(inspected) && matchesAny(decodedOnce);
    }

    private boolean matchesAny(String value) {
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    private String urlDecodeSafely(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException ex) {
            return value;
        } catch (Exception ex) {
            return value;
        }
    }

    private String safeForLog(String value) {
        if (value == null) {
            return "null";
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]", "_");
        return sanitized.length() > 512 ? sanitized.substring(0, 512) + "..." : sanitized;
    }
}