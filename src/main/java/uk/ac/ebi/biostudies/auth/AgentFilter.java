package uk.ac.ebi.biostudies.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AgentFilter implements Filter {

    @Value("${denied.agent}")
    private String deniedAgent;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String userAgent = httpRequest.getHeader("User-Agent");

            if (userAgent != null && StringUtils.hasText(deniedAgent) && (userAgent.toLowerCase().contains(deniedAgent.toLowerCase()))) {
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
