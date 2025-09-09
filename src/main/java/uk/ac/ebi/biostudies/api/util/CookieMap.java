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

package uk.ac.ebi.biostudies.api.util;

import javax.servlet.http.Cookie;
import java.util.HashMap;

public class CookieMap extends HashMap<String, Cookie> {
  public CookieMap(Cookie[] cookies) {
    if (null != cookies) {
      for (Cookie c : cookies) {
        this.put(c.getName(), c);
      }
    }
  }

  public String getCookieValue(String name) {
    if (this.containsKey(name)) {
      return this.get(name).getValue();
    }
    return null;
  }
}
