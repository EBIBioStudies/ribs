package uk.ac.ebi.biostudies.efo;

/*
 * Copyright 2009-2014 European Molecular Biology Laboratory
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EFOImpl implements IEFO {
  private final Map<String, EFONode> efoMap = new HashMap<String, EFONode>();
  private final Map<String, Set<String>> partOfIdMap = new HashMap<String, Set<String>>();
  private final String versionInfo;

  public EFOImpl(String versionInfo) {
    this.versionInfo = versionInfo;
  }

  public Map<String, EFONode> getMap() {
    return this.efoMap;
  }

  public Map<String, Set<String>> getPartOfIdMap() {
    return this.partOfIdMap;
  }

  public Set<String> getTerms(String efoId, int includeFlags) {
    EFONode node = getMap().get(efoId);
    return getTerms(node, includeFlags);
  }

  private Set<String> getTerms(EFONode node, int includeFlags) {
    Set<String> terms = new HashSet<String>();
    if (null != node) {
      if ((includeFlags & INCLUDE_SELF) > 0) {
        terms.add(node.getTerm());
      }

      if ((includeFlags & INCLUDE_ALT_TERMS) > 0) {
        terms.addAll(node.getAlternativeTerms());
      }

      if ((includeFlags & INCLUDE_CHILD_TERMS) > 0) {
        if (node.hasChildren()) {
          for (EFONode child : node.getChildren()) {
            terms.addAll(
                getTerms(
                    child,
                    includeFlags
                        | INCLUDE_SELF
                        | ((includeFlags & INCLUDE_CHILD_ALT_TERMS) > 0 ? INCLUDE_ALT_TERMS : 0)));
          }
        }
      }

      if ((includeFlags & INCLUDE_PART_OF_TERMS) > 0) {
        if (getPartOfIdMap().containsKey(node.getId())) {
          for (String partOfId : getPartOfIdMap().get(node.getId())) {
            EFONode partOfNode = getMap().get(partOfId);
            terms.addAll(
                getTerms(
                    partOfNode,
                    includeFlags
                        | INCLUDE_SELF
                        | ((includeFlags & INCLUDE_CHILD_ALT_TERMS) > 0 ? INCLUDE_ALT_TERMS : 0)));
          }
        }
      }
    }

    return terms;
  }

  public String getVersionInfo() {
    return this.versionInfo;
  }
}
