package uk.ac.ebi.biostudies.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Properties;

@Service
public class TextService implements InitializingBean, DisposableBean {

  private final Logger logger = LogManager.getLogger(TextService.class.getName());
  Properties props = null;

  public String getNormalisedString(String string) {
    return props.containsKey(string) ? props.getProperty(string) : string;
  }

  @Override
  public void destroy() {}

  @Override
  public void afterPropertiesSet() {
    logger.debug("Initialising TextService");
    Resource resource = new ClassPathResource("normalised-text.properties");
    try {
      props = PropertiesLoaderUtils.loadProperties(resource);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
