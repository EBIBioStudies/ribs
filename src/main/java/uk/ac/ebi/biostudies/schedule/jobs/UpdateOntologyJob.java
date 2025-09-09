/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.biostudies.schedule.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.efo.EFOLoader;
import uk.ac.ebi.biostudies.api.util.EmailSender;
import uk.ac.ebi.biostudies.config.EFOConfig;
import uk.ac.ebi.biostudies.config.MailConfig;
import uk.ac.ebi.biostudies.efo.StringTools;
import uk.ac.ebi.biostudies.efo.index.EFOManager;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.StandardCopyOption;

@Service
public class UpdateOntologyJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOntologyJob.class);

  @Autowired EFOManager efoManager;
  @Autowired EFOConfig efoConfig;
  @Autowired EmailSender emailSender;
  @Autowired MailConfig mailConfig;

  @Scheduled(cron = "${bs.efo.update}")
  public void doExecute() throws Exception {
    // check the version of EFO from update location; if newer, copy it
    // over to our location and launch a reload process
    String efoLocation = efoConfig.getUrl(); // getPreferences().getString("bs.efo.update.source");
    URI efoURI = new URI(efoLocation);
    LOGGER.info("Checking EFO ontology version from [{}]", efoURI);
    String version = EFOLoader.getOWLVersion(efoURI);
    String loadedVersion = null;
    if (efoManager.getEfo() != null) loadedVersion = efoManager.getEfo().getVersionInfo();
    if (loadedVersion == null
        || (null != version
            && !version.equals(loadedVersion)
            && isVersionNewer(version, loadedVersion))) {

      // we have newer version, let's fetch it and copy it over to our working location
      LOGGER.info("Updating EFO with version [{}]", version);

      try (InputStream is = efoURI.toURL().openStream()) {
        File efoFile = new File(efoConfig.getOwlFilename());
        java.nio.file.Files.copy(is, efoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        try {
          emailSender.send(
              mailConfig.getReportsRecipients(),
              mailConfig.getHiddenRecipients(),
              "EFO update",
              "Experimental Factor Ontology has been updated to version ["
                  + version
                  + "]"
                  + StringTools.EOL
                  + StringTools.EOL
                  + "Application [${variable.appname}]"
                  + StringTools.EOL
                  + "Host [${variable.hostname}]"
                  + StringTools.EOL,
              mailConfig.getReportsOriginator());
        } catch (Throwable e) {
          LOGGER.debug("Problem in sending email for EFO update", e);
        }
        try {
          efoManager.loadEfo();
          efoManager.buildIndex();
        } catch (Throwable throwable) {
          LOGGER.error("Unable to load EFO ontology file and create EFO Index", throwable);
        }
        LOGGER.info("EFO has updated");
      }
    }
  }

  private static boolean isVersionNewer(String version, String baseVersion) {
    if (version == null || version.isEmpty()) return false;
    if (baseVersion == null || baseVersion.isEmpty()) return false;
    String[] versionArr = version.split("\\.");
    String[] baseVersionArr = baseVersion.split("\\.");
    int baseVer, newVer;
    try {
      for (int i = 0; i < versionArr.length; i++) {
        baseVer = Integer.parseInt(baseVersionArr[i]);
        newVer = Integer.parseInt(versionArr[i]);
        if (newVer > baseVer) return true;
        if (newVer < baseVer) return false;
      }
      return false;
    } catch (Throwable error) {
      LOGGER.error(
          "problem in parsing new version or base version new:{} and base:{}",
          version,
          baseVersion);
      return false;
    }
  }
}
