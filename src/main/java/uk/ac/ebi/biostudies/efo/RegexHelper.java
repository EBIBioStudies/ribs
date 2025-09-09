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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexHelper {
  private Pattern pattern = null;
  private String flags = "";

  public RegexHelper(String regexp) {
    pattern = Pattern.compile(regexp);
  }

  public RegexHelper(String regexp, String aFlags) {
    if (null != aFlags) {
      flags = aFlags.toLowerCase();
    }

    if (null != regexp) {
      pattern =
          Pattern.compile(
              regexp,
              (flags.contains("m") ? Pattern.MULTILINE : 0)
                  + (flags.contains("i") ? Pattern.CASE_INSENSITIVE : 0));
    }
  }

  public boolean test(String input) {
    return (null != pattern) && (null != input) && pattern.matcher(input).find();
  }

  public String matchFirst(String input) {
    if (null != pattern && null != input) {
      Matcher matcher = pattern.matcher(input);
      if (matcher.find() && 0 < matcher.groupCount()) {
        return matcher.group(1);
      }
    }
    return "";
  }

  public String[] match(String input) {
    String[] result = null;
    if (null != pattern && null != input) {
      Matcher matcher = pattern.matcher(input);
      if (matcher.find() && 0 < matcher.groupCount()) {
        result = new String[matcher.groupCount()];
        for (int i = 1; i <= matcher.groupCount(); ++i) {
          result[i - 1] = matcher.group(i);
        }
      }
    }
    return result;
  }

  public String replace(String input, String replace) {
    if (null == replace) {
      replace = "";
    }
    if (null != pattern && null != input) {
      Matcher matcher = pattern.matcher(input);
      return flags.contains("g") ? matcher.replaceAll(replace) : matcher.replaceFirst(replace);
    } else {
      return input;
    }
  }
}
