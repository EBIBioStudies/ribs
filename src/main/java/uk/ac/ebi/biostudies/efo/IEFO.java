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

import java.util.Map;
import java.util.Set;

public interface IEFO {
  int INCLUDE_SELF = 1;
  int INCLUDE_ALT_TERMS = 2;
  int INCLUDE_CHILD_TERMS = 4;
  int INCLUDE_CHILD_ALT_TERMS = 8;
  int INCLUDE_PART_OF_TERMS = 16;

  int INCLUDE_ALL =
      INCLUDE_SELF
          + INCLUDE_ALT_TERMS
          + INCLUDE_CHILD_TERMS
          + INCLUDE_CHILD_ALT_TERMS
          + INCLUDE_PART_OF_TERMS;

  int INCLUDE_CHILDREN =
      INCLUDE_CHILD_TERMS + INCLUDE_CHILD_ALT_TERMS + INCLUDE_PART_OF_TERMS;

  String ROOT_ID = "http://www.ebi.ac.uk/efo/EFO_0000001";

  Map<String, EFONode> getMap();

  Map<String, Set<String>> getPartOfIdMap();

  Set<String> getTerms(String efoId, int includeFlags);

  String getVersionInfo();
}
