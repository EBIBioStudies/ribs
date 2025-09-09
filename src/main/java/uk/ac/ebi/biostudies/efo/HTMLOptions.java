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


import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.text.StringEscapeUtils;

public class HTMLOptions {
    private class Option {
        private final String value;
        private final String label;

        public Option(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getHtml() {
            return "<option value=\"" + StringEscapeUtils.escapeHtml3(value) + "\">" + label + "</option>";
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof Option) && ((Option) other).getHtml().equals(getHtml());
        }
    }

    private final Set<Option> options;

    public HTMLOptions() {
        this.options = new LinkedHashSet<>();
    }

    public void clearOptions() {
        this.options.clear();
    }

    public void addOption(String value, String label) {
        this.options.add(new Option(value, label));
    }

    public String getHtml() {
        StringBuilder sb = new StringBuilder();

        for (Option o : options) {
            sb.append(o.getHtml());
        }

        return sb.toString();
    }
}
