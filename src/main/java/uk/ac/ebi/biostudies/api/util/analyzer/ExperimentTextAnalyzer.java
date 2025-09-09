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

package uk.ac.ebi.biostudies.api.util.analyzer;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.util.CharTokenizer;
import uk.ac.ebi.biostudies.config.IndexConfig;

public final class ExperimentTextAnalyzer extends Analyzer {
  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    Tokenizer source = new ExperimentTextTokenizer();
    TokenStream filter = new StopFilter(new ASCIIFoldingFilter(source), IndexConfig.STOP_WORDS);
    filter = new LowerCaseFilter(filter);
    return new TokenStreamComponents(source, filter);
  }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new LowerCaseFilter(in);
  }

  private static class ExperimentTextTokenizer extends CharTokenizer {
    @Override
    protected boolean isTokenChar(int c) {
      return Character.isLetter(c) | Character.isDigit(c) | ('-' == c);
    }
  }
}
