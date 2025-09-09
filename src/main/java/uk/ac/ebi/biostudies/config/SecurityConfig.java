package uk.ac.ebi.biostudies.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import uk.ac.ebi.biostudies.api.util.HttpTools;

/** Created by ehsan on 16/03/2017. */
@Configuration
@PropertySource("classpath:security.properties")
public class SecurityConfig implements InitializingBean {

  @Override
  public void afterPropertiesSet() {
    HttpTools.PROXY_HOST = getHttpProxyHost();
    HttpTools.PROXY_PORT = getHttpProxyPort();
  }

  @Value("${auth.profileUrl}")
  private String profileUrl;

  @Value("${partial.update.rest.token}")
  private String partialUpdateRestSecurityToken;

  @Value("${auth.loginUrl}")
  private String loginUrl;

  @Value("${index.admin.ip.allow.list}")
  private String adminIPAllowList;

  @Value("${aspera.user}")
  private String asperaUser;

  @Value("${aspera.pass}")
  private String asperaPass;

  @Value("${aspera.token.server}")
  private String asperaTokenServer;

  @Value("${http.proxy.host}")
  private String httpProxyHost;

  @Value("${http.proxy.port}")
  private Integer httpProxyPort;

  @Value("${stomp.login.header}")
  private String stompLoginUser;

  @Value("${stomp.password.header}")
  private String stompPassword;

  @Value("${stomp.host}")
  private String stompHost;

  public String getStompLoginUser() {
    return stompLoginUser;
  }

  public String getStompPassword() {
    return stompPassword;
  }

  public String getStompHost() {
    return stompHost;
  }

  public String getStompPort() {
    return stompPort;
  }

  @Value("${stomp.port}")
  private String stompPort;

  public String getProfileUrl() {
    return profileUrl;
  }

  public String getLoginUrl() {
    return loginUrl;
  }

  public String getAdminIPAllowList() {
    return adminIPAllowList;
  }

  public String getAsperaUser() {
    return asperaUser;
  }

  public String getAsperaPass() {
    return asperaPass;
  }

  public String getAsperaTokenServer() {
    return asperaTokenServer;
  }

  public String getHttpProxyHost() {
    return httpProxyHost;
  }

  public Integer getHttpProxyPort() {
    return httpProxyPort;
  }

  public String getPartialUpdateRestSecurityToken() {
    return partialUpdateRestSecurityToken;
  }
}
