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

import java.util.Set;
import java.util.TreeSet;

public class EFOExpansionTerms {
  public String term;
  public Set<String> synonyms;
  public Set<String> efo;

  public EFOExpansionTerms() {
    term = "";
    synonyms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    efo = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
  }
}
