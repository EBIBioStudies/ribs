package uk.ac.ebi.biostudies.config;

import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * XSS Filter to sanitize all incoming HTTP requests and prevent XSS attacks.
 * This filter wraps the HttpServletRequest to sanitize parameters, headers, and request URI.
 */
@Component
public class XSSFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(XSSFilter.class);

    // Patterns to detect potential XSS attacks
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onerror(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onmouseover(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onfocus(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onblur(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onchange(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onclick(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("<iframe", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<link", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<meta", Pattern.CASE_INSENSITIVE)
    };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("XSSFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Check if the request URI itself contains XSS attempts
        String requestURI = httpRequest.getRequestURI();
        if (containsXSS(requestURI)) {
            logger.warn("XSS attempt detected in URI: {}", requestURI);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Wrap the request to sanitize parameters
        XSSRequestWrapper wrappedRequest = new XSSRequestWrapper(httpRequest);
        chain.doFilter(wrappedRequest, response);
    }

    @Override
    public void destroy() {
        logger.info("XSSFilter destroyed");
    }

    /**
     * Check if the input string contains potential XSS patterns
     */
    private boolean containsXSS(String value) {
        if (value == null) {
            return false;
        }

        // Decode URL encoding first
        String decodedValue = java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(decodedValue).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sanitize the input string using OWASP Encoder
     */
    private String sanitizeInput(String value) {
        if (value == null) {
            return null;
        }

        // First check if it contains XSS patterns
        if (containsXSS(value)) {
            logger.warn("XSS attempt detected and sanitized: {}", value);
            // For URLs and parameters that contain XSS, we encode them
            return Encode.forHtml(value);
        }

        // For normal content, apply basic HTML encoding
        return Encode.forHtml(value);
    }

    /**
     * Custom HttpServletRequestWrapper to sanitize request parameters
     */
    public class XSSRequestWrapper extends HttpServletRequestWrapper {

        public XSSRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String parameter) {
            String[] values = super.getParameterValues(parameter);
            if (values == null) {
                return null;
            }

            String[] encodedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                encodedValues[i] = sanitizeInput(values[i]);
            }
            return encodedValues;
        }

        @Override
        public String getParameter(String parameter) {
            String value = super.getParameter(parameter);
            return sanitizeInput(value);
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return sanitizeInput(value);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            Vector<String> sanitizedHeaders = new Vector<>();
            Enumeration<String> headers = super.getHeaders(name);

            while (headers.hasMoreElements()) {
                String header = headers.nextElement();
                sanitizedHeaders.add(sanitizeInput(header));
            }

            return sanitizedHeaders.elements();
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> originalMap = super.getParameterMap();
            Map<String, String[]> sanitizedMap = new HashMap<>();

            for (Map.Entry<String, String[]> entry : originalMap.entrySet()) {
                String[] originalValues = entry.getValue();
                String[] sanitizedValues = new String[originalValues.length];

                for (int i = 0; i < originalValues.length; i++) {
                    sanitizedValues[i] = sanitizeInput(originalValues[i]);
                }

                sanitizedMap.put(entry.getKey(), sanitizedValues);
            }

            return sanitizedMap;
        }

        @Override
        public String getQueryString() {
            String queryString = super.getQueryString();
            return sanitizeInput(queryString);
        }
    }
}
