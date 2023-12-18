package uk.ac.ebi.biostudies.auth;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BotFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code here (if needed)
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String userAgent = httpRequest.getHeader("User-Agent");

            if (userAgent != null && (userAgent.toLowerCase().contains("bot") || userAgent.toLowerCase().contains("python-requests"))) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.getWriter().write("403 Forbidden - Access denied");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Cleanup code here (if needed)
    }


}
