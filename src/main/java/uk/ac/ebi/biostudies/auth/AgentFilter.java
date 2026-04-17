package uk.ac.ebi.biostudies.auth;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AgentFilter extends OncePerRequestFilter {

  @Value("${denied.agents:}")
  private String deniedAgentsRaw;

  private List<Pattern> deniedAgentPatterns;

  private List<String> deniedAgents;

  @Override
  protected void initFilterBean() {
    deniedAgentPatterns =
        Stream.of(deniedAgentsRaw.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(s -> Pattern.compile("\\b" + Pattern.quote(s.toLowerCase(Locale.ROOT)) + "\\b"))
            .collect(Collectors.toList());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String ua = request.getHeader("User-Agent");

    if (StringUtils.hasText(ua) && !deniedAgentPatterns.isEmpty()) {
      String uaLower = ua.toLowerCase(Locale.ROOT);

      boolean blocked = deniedAgentPatterns.stream().anyMatch(p -> p.matcher(uaLower).find());

      if (blocked) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getWriter().write("403 Forbidden – User-Agent blocked");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
