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

import au.com.bytecode.opencsv.CSVReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynonymsFileReader extends CSVReader {
    // logging machinery
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public SynonymsFileReader(Reader ioReader) {
        super(ioReader, ',', '\"');
    }

    public Map<String, Set<String>> readSynonyms() throws IOException {
        Map<String, Set<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<String[]> lines = this.readAll();
        for (String[] line : lines) {
            if (null != line && line.length > 0) {
                if ('#' != line[0].charAt(0)) {
                    Set<String> syns = new HashSet<>(Arrays.asList(line));
                    for (String syn : line) {
                        if (result.containsKey(syn)) {
                            // already have one synonym in the list, need to merge sets
                            Set<String> existingSyns = result.get(syn);
                            syns.addAll(existingSyns);
                            this.logger.warn("Synonym [{}] already exists, merging sets", syn);
                        }
                    }
                    for (String syn : syns) {
                        result.put(syn, syns);
                    }
                }
            }
        }
        return result;
    }
}
