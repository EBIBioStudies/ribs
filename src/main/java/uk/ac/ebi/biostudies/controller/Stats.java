package uk.ac.ebi.biostudies.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biostudies.service.SearchService;
import static uk.ac.ebi.biostudies.api.util.Constants.JSON_UNICODE_MEDIA_TYPE;

/**
 * Created by awais on 14/02/2017.
 *
 * <p>Rest endpoint for searching Biostudies
 */
@RestController
@RequestMapping(value = "/api/v1")
public class Stats {

  private final Logger logger = LogManager.getLogger(Stats.class.getName());
  public static final String LATEST_ENDPOINT = "/latest";
  public static final String STATS_ENDPOINT = "/stats";

  @Autowired SearchService searchService;

  @RequestMapping(
      value = LATEST_ENDPOINT,
      produces = JSON_UNICODE_MEDIA_TYPE,
      method = RequestMethod.GET)
  public String getLatestStudies() throws Exception {
    return searchService.getLatestStudies();
  }

  @RequestMapping(
      value = STATS_ENDPOINT,
      produces = JSON_UNICODE_MEDIA_TYPE,
      method = RequestMethod.GET)
  public String getStats() throws Exception {
    return searchService.getFieldStats();
  }
}
