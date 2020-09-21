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

package uk.ac.ebi.biostudies.efo.autocompletion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class AutocompleteStore {
    private SetTrie trie;
    private HashMap<String, AutocompleteData> objects;

    private static class AutocompleteComparator implements Comparator<String>, Serializable {
        public int compare(String s1, String s2) {
            // the ultimate idea of this comparison is to get ontology terms before keywords
            //
            //
            //
            int pos1 = s1.lastIndexOf('|');
            int pos2 = s2.lastIndexOf('|');

            String text1 = s1.toLowerCase().substring(0, (-1 != pos1 ? pos1 : s1.length()));
            String text2 = s2.toLowerCase().substring(0, (-1 != pos2 ? pos2 : s2.length()));

            int comp = text1.compareTo(text2);
            if (0 == comp) {
                if (-1 == pos1 && -1 != pos2) {
                    comp = -1;
                } else if (-1 != pos1 && -1 == pos2) {
                    comp = 1;
                } else if ((pos1 >= s1.length() - 1) || (pos1 >= s2.length() - 1)) {
                    comp = s1.compareToIgnoreCase(s2);
                } else {
                    comp = s1.charAt(pos1 + 1) - s2.charAt(pos2 + 1);
                    if (0 == comp) {
                        comp = s1.compareToIgnoreCase(s2);
                    }
                }
            }
            return comp;
        }
    }

    public AutocompleteStore() {
        this.trie = new SetTrie(new AutocompleteComparator());
        this.objects = new HashMap<>();
    }

    public void clear() {
        trie.clear();
        objects.clear();
    }

    public void addData(AutocompleteData data) {
        String key = data.getText() + "|" + data.getDataType() + "_" + data.getData();

        this.trie.add(key);
        this.objects.put(key, data);
    }

    public List<AutocompleteData> findCompletions(String prefix, String fieldName, Integer limit) {
        List<AutocompleteData> comps = new ArrayList<>();
        if ("".equals(fieldName) || !" assaycount samplecount rawcount processedcount efcount sacount miamescore ".contains(" " + fieldName + " ")) {
            List<String> matches = trie.findCompletions(prefix);

            for (String key : matches) {
                AutocompleteData data = this.objects.get(key);
                boolean shouldAdd = false;
                if ("".equals(fieldName)) {
                    // in this case we put "keyword" data, EFO and fieldNames, EFO will override keywords
                    if (    AutocompleteData.DATA_TEXT.equals(data.getDataType())
                                 && ("content collection title".contains(data.getData().toLowerCase())
                            ) || !AutocompleteData.DATA_TEXT.equals(data.getDataType())) {
                        shouldAdd = true;
                    }
                } else {
                    if ((AutocompleteData.DATA_TEXT.equals(data.getDataType()) && fieldName.equals(data.getData()))
                            || (" sa efv exptype ".contains(" " + fieldName + " ") && AutocompleteData.DATA_EFO_NODE.equals(data.getDataType()))) {
                        shouldAdd = true;
                    }
                }
                // so if we want to add this match, check if we have the same text in the list already
                // if we do, then either discard this one (if it's from the index), or swap with the match (if efo)
                // or still add (field)
                if (shouldAdd) {
                    for (int compIndex = 0; compIndex < comps.size(); ++compIndex) {
                        if (comps.get(compIndex).getText().equals(data.getText())) {
                            if (AutocompleteData.DATA_EFO_NODE.equals(comps.get(compIndex).getDataType())
                                    && AutocompleteData.DATA_TEXT.equals(data.getDataType())) {
                                shouldAdd = false;
                                break;
                            } else if (AutocompleteData.DATA_TEXT.equals(comps.get(compIndex).getDataType())
                                    && AutocompleteData.DATA_EFO_NODE.equals(data.getDataType())) {
                                comps.set(compIndex, data);
                                shouldAdd = false;
                                break;
                            }
                        }
                    }
                    if (shouldAdd) {
                        comps.add(data);
                    }
                }
                if (null != limit && comps.size() == limit) {
                    break;
                }
            }
        }
        return comps;
    }
}
