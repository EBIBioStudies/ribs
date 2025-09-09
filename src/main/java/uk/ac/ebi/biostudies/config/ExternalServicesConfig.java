package uk.ac.ebi.biostudies.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Created by ehsan on 16/03/2017. */
@Configuration
@PropertySource("classpath:external-services.properties")
public class ExternalServicesConfig {

  @Value("${git.commit.id.abbrev}")
  private String gitCommitIdAbbrev;

  @Value("${dataclaiming.url}")
  private String dataClaimingUrl;

  @Value("${analytics.code}")
  private String analyticsCode;

  @Value("${access.control.allow.origin}")
  private String accessControlAllowOrigin;

  public String getdataClaimingUrl() {
    return dataClaimingUrl;
  }

  public String getAnalyticsCode() {
    return analyticsCode;
  }

  public String getGitCommitIdAbbrev() {
    return gitCommitIdAbbrev;
  }

  public String getAccessControlAllowOrigin() {
    return accessControlAllowOrigin;
  }
}
