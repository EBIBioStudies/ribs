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

package uk.ac.ebi.biostudies.efo;

import org.apache.logging.log4j.LogManager;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.analyzer.ExperimentTextAnalyzer;
import uk.ac.ebi.biostudies.config.IndexConfig;

@Component
public class EFOExpandedHighlighter {
    // logging machinery
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(EFOExpandedHighlighter.class.getName());

    @Autowired
    IndexConfig indexConfig;

    private static final String HIT_OPEN_MARK = "\u00ab";
    private static final String HIT_CLOSE_MARK = "\u00bb";
    private static final String SYN_OPEN_MARK = "\u2039";
    private static final String SYN_CLOSE_MARK = "\u203a";
    private static final String EFO_OPEN_MARK = "\u2037";
    private static final String EFO_CLOSE_MARK = "\u2034";

    private final RegexHelper SYN_AND_HIT_REGEX = new RegexHelper(HIT_OPEN_MARK + SYN_OPEN_MARK + "([^" + SYN_CLOSE_MARK + HIT_CLOSE_MARK + "]+)" + SYN_CLOSE_MARK + HIT_CLOSE_MARK, "g");
    private final RegexHelper EFO_AND_SYN_REGEX = new RegexHelper(SYN_OPEN_MARK + EFO_OPEN_MARK + "([^" + EFO_CLOSE_MARK + SYN_CLOSE_MARK + "]+)" + EFO_CLOSE_MARK + SYN_CLOSE_MARK, "g");
    private final RegexHelper EFO_AND_HIT_REGEX = new RegexHelper(HIT_OPEN_MARK + EFO_OPEN_MARK + "([^" + EFO_CLOSE_MARK + HIT_CLOSE_MARK + "]+)" + EFO_CLOSE_MARK + HIT_CLOSE_MARK, "g");


    public String highlightQuery(Query originalQuery, String fieldName, String text, boolean fragmentOnly ) {

        String result = doHighlightQuery(originalQuery, fieldName, text, "", "", fragmentOnly);

        return result;

    }


    private String doHighlightQuery(Query query, String fieldName, String text, String openMark,
                                    String closeMark, boolean fragmentOnly) {
        try {
            SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter(openMark, closeMark);
            QueryScorer scorer = new QueryScorer(query, fieldName, indexConfig.getDefaultField());
            Highlighter highlighter = new Highlighter(htmlFormatter, scorer);
            highlighter.setTextFragmenter( fragmentOnly
                    ? new SimpleSpanFragmenter(scorer, indexConfig.getSearchSnippetFragmentSize())
                    : new NullFragmenter());
            String str = highlighter.getBestFragment(new ExperimentTextAnalyzer(), "".equals(fieldName)
                                    ? indexConfig.getDefaultField()
                                    : fieldName, text);
            return str;
        } catch (Exception x) {
            logger.error("Caught an exception:", x);
        }
        return text;

    }
}
