package uk.ac.ebi.biostudies.config;

import static uk.ac.ebi.biostudies.api.util.Constants.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Created by ehsan on 09/03/2017.
 */
@Component
@Scope("singleton")
public class TaxonomyManager {
    private final Logger logger = LogManager.getLogger(TaxonomyManager.class.getName());

    private TaxonomyWriter taxonomyWriter;
    private TaxonomyReader taxonomyReader;
    private FacetsConfig facetsConfig;
    private Directory taxoDirectory;
    public JsonNode PROJECT_FACET;

    @Autowired
    IndexConfig indexConfig;

    public void init(Collection<JsonNode> allFields){
        facetsConfig = new FacetsConfig();

        for (JsonNode jsonNode : allFields) {
            if (jsonNode.get(IndexEntryAttributes.FIELD_TYPE).textValue().equalsIgnoreCase(IndexEntryAttributes.FieldTypeValues.FACET)) {
                if (!jsonNode.has(IndexEntryAttributes.MULTIVALUED)
                        || jsonNode.get(IndexEntryAttributes.MULTIVALUED).asBoolean(true)) {
                    getFacetsConfig().setMultiValued(jsonNode.get(IndexEntryAttributes.NAME).asText(), true);
                }
                if (jsonNode.get(IndexEntryAttributes.NAME).textValue().equalsIgnoreCase(Facets.COLLECTION)) {
                    PROJECT_FACET = jsonNode;
                }
            }
        }

    }

    public FacetsConfig getFacetsConfig() {
        return facetsConfig;
    }
}
