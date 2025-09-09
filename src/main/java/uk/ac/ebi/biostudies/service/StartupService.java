package uk.ac.ebi.biostudies.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.config.IndexManager;

@Service
public class StartupService implements InitializingBean {
  private static final Logger LOGGER = LogManager.getLogger(StartupService.class.getName());

  @Autowired IndexManager indexManager;
  @Autowired IndexService indexService;
  @Autowired IndexManagementService indexManagementService;
  @Autowired IndexConfig indexConfig;

  @Override
  public void afterPropertiesSet() {
    LOGGER.debug("Initializing IndexManagerService");
    try {
      indexManager.refreshIndexWriterAndWholeOtherIndices();
      if (indexConfig.isApiEnabled()) indexService.processFileForIndexing();
      indexManagementService.openWebsocket();
    } catch (Throwable ex) {
      LOGGER.error("problem in initiating indices", ex);
    }
  }
}
