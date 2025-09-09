package uk.ac.ebi.biostudies.api.util.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static uk.ac.ebi.biostudies.api.util.Constants.*;

/** Created by ehsan on 27/03/2017. */
@Service
public class AnalyzerManager {
  private Logger logger = LogManager.getLogger(AnalyzerManager.class.getName());

  private Map<String, Analyzer> perFieldAnalyzerMap = new HashMap<>();
  private PerFieldAnalyzerWrapper perFieldAnalyzerWrapper;
  private Map<String, String> expandableFields = new HashMap<>();

  public void init(Map<String, JsonNode> allFields) {
    for (String key : allFields.keySet()) {
      JsonNode curField = allFields.get(key);
      if (curField
          .get(IndexEntryAttributes.FIELD_TYPE)
          .asText()
          .equalsIgnoreCase(IndexEntryAttributes.FieldTypeValues.FACET)) continue;
      String analyzer =
          curField.has(IndexEntryAttributes.ANALYZER)
              ? curField.get(IndexEntryAttributes.ANALYZER).asText()
              : null;
      boolean expand =
          curField.has(IndexEntryAttributes.EXPANDED)
              ? curField.get(IndexEntryAttributes.EXPANDED).asBoolean(false)
              : false;
      if (analyzer != null && !analyzer.isEmpty()) {
        analyzer = this.getClass().getPackage().getName() + "." + analyzer;
        Class<?> clazz;
        try {
          clazz = Class.forName(analyzer);
          Analyzer analyzerObj = (Analyzer) clazz.getDeclaredConstructor().newInstance();
          perFieldAnalyzerMap.put(key, analyzerObj);
        } catch (Exception e) {
          logger.error("cant create analyzer with name {}", analyzer, e);
        }
      }
      if (expand) {
        expandableFields.put(key, key);
      }
    }
    perFieldAnalyzerWrapper =
        new PerFieldAnalyzerWrapper(new AttributeFieldAnalyzer(), perFieldAnalyzerMap);
  }

  public PerFieldAnalyzerWrapper getPerFieldAnalyzerWrapper() {
    return perFieldAnalyzerWrapper;
  }

  public Map<String, String> getExpandableFields() {
    return expandableFields;
  }
}
