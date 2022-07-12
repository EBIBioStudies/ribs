package uk.ac.ebi.biostudies.api.util.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.ReadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.DateTools;
import uk.ac.ebi.biostudies.api.util.Constants;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static uk.ac.ebi.biostudies.api.util.Constants.PUBLIC;
import static uk.ac.ebi.biostudies.api.util.Constants.RELEASE_DATE;

public class DateParser extends AbstractParser {
    private static final Logger logger = LogManager.getLogger(DateParser.class.getName());

    @Override
    public String parse(Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Long releaseDateLong = null;
        long creationDateLong = 0L;
        long modificationTimeLong = 0L;

        if (submission.has(Constants.Fields.CREATION_TIME)) {
            creationDateLong = Long.parseLong(submission.get(Constants.Fields.CREATION_TIME).asText()) * 1000;
        } else if (submission.has(Constants.Fields.CREATION_TIME_FULL)) {
            if (submission.get(Constants.Fields.CREATION_TIME_FULL).isObject() && submission.get(Constants.Fields.CREATION_TIME_FULL).has("$date")) {
                creationDateLong = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(submission.get(Constants.Fields.CREATION_TIME_FULL).get("$date").textValue())).toEpochMilli();
            } else {
                creationDateLong = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(submission.get(Constants.Fields.CREATION_TIME_FULL).asText())).toEpochMilli();
            }
        }
        valueMap.put(Constants.Fields.CREATION_TIME, creationDateLong);

        if (submission.has(Constants.Fields.MODIFICATION_TIME)) {
            modificationTimeLong = Long.parseLong(submission.get(Constants.Fields.MODIFICATION_TIME).asText()) * 1000;
        } else if (submission.has(Constants.Fields.MODIFICATION_TIME_FULL)) {
            if (submission.get(Constants.Fields.MODIFICATION_TIME_FULL).isObject() && submission.get(Constants.Fields.MODIFICATION_TIME_FULL).has("$date")) {
                modificationTimeLong = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(submission.get(Constants.Fields.MODIFICATION_TIME_FULL).get("$date").textValue())).toEpochMilli();
            } else {
                modificationTimeLong = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(submission.get(Constants.Fields.MODIFICATION_TIME_FULL).asText())).toEpochMilli();
            }
        }
        if (modificationTimeLong != 0L) {
            valueMap.put(Constants.Fields.MODIFICATION_TIME, modificationTimeLong);
            valueMap.put(Constants.Facets.MODIFICATION_YEAR_FACET, DateTools.timeToString(modificationTimeLong, DateTools.Resolution.YEAR));
        }

        if (submission.has(Constants.Fields.RELEASE_TIME)) {
            if (!submission.get(Constants.Fields.RELEASE_TIME).asText().equals("-1") && !submission.get(Constants.Fields.RELEASE_TIME).asText().equals("null")) {
                releaseDateLong = Long.parseLong(submission.get(Constants.Fields.RELEASE_TIME).asText()) * 1000;
            } else if (String.valueOf(valueMap.get(Constants.Fields.ACCESS)).contains(PUBLIC)) {
                releaseDateLong = Long.parseLong(submission.get(Constants.Fields.MODIFICATION_TIME).asText()) * 1000;
            }
        } else if (submission.has(Constants.Fields.RELEASE_TIME_FULL)) {
            if (submission.get(Constants.Fields.RELEASE_TIME_FULL).isObject() && submission.get(Constants.Fields.RELEASE_TIME_FULL).has("$date")) {
                if (submission.get(Constants.Fields.RELEASE_TIME_FULL).get("$date").has("$numberLong")) { // date is before epoch
                    releaseDateLong = Long.parseLong(submission.get(Constants.Fields.RELEASE_TIME_FULL).get("$date").get("$numberLong").textValue());
                } else {
                    releaseDateLong = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(submission.get(Constants.Fields.RELEASE_TIME_FULL).get("$date").textValue())).toEpochMilli();
                }
            } else if (!submission.get(Constants.Fields.RELEASE_TIME_FULL).asText().equals("-1") && !submission.get(Constants.Fields.RELEASE_TIME_FULL).asText().equals("null")) {
                Instant instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(submission.get(Constants.Fields.RELEASE_TIME_FULL).asText()));
                releaseDateLong = instant.toEpochMilli();
            } else if (String.valueOf(valueMap.get(Constants.Fields.ACCESS)).contains(PUBLIC)) {
                releaseDateLong = (long) valueMap.get(Constants.Fields.MODIFICATION_TIME);
            }
        }

        valueMap.put(Constants.Fields.RELEASE_TIME, releaseDateLong);
        valueMap.put(RELEASE_DATE, releaseDateLong==null ? Constants.NA : simpleDateFormat.format(DateTools.round(releaseDateLong, DateTools.Resolution.DAY)));
        valueMap.put(Constants.Facets.RELEASED_YEAR_FACET, releaseDateLong==null ? Constants.NA :DateTools.timeToString(releaseDateLong, DateTools.Resolution.YEAR));
        return "";
    }
}
