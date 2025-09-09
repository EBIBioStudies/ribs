package uk.ac.ebi.biostudies.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import uk.ac.ebi.biostudies.config.IndexManager;

/**
 * Created by awais on 16/02/2017.
 */
public class BioStudiesQueryParser extends MultiFieldQueryParser {


    private static IndexManager indexManager;

    public BioStudiesQueryParser(String[] fields, Analyzer analyzer, IndexManager indexManager) {
        super(fields, analyzer);
        BioStudiesQueryParser.indexManager = indexManager;
    }

    @Override
    public Query parse(String query) throws ParseException {
        if ( (" "+query.toLowerCase()).contains(" access:")) {
            throw new ParseException("Field 'access' not allowed in queries.");
        }
        return super.parse(query);
    }

    @Override
    protected Query getRangeQuery(String field, String min, String max, boolean startInclusive, boolean endInclusive) throws ParseException {

        Map<String, JsonNode> allValidFields = indexManager.getIndexEntryMap();
        if (allValidFields.containsKey(field)) {
            if (allValidFields.get(field).get(Constants.IndexEntryAttributes.FIELD_TYPE).textValue().equals(Constants.IndexEntryAttributes.FieldTypeValues.LONG)) {
                return LongPoint.newRangeQuery(field, Long.parseLong(min), Long.parseLong(max));
            }
        }
        return super.getRangeQuery(field, min, max, startInclusive, endInclusive);
    }
}