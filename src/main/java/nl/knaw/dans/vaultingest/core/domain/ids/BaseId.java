/*
 * Copyright (C) 2023 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.vaultingest.core.domain.ids;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public abstract class BaseId {
    private final String id;
    private final Pattern pattern = Pattern
        .compile("https?://.*/([-0-9]+[0-9Xx])$");

    protected BaseId(String id) {
        this.id = id;
    }

    protected String reduceUriToId(String value) {
        if (!StringUtils.isAllEmpty(value)) {
            var m = pattern.matcher(value);
            if (m.find())
                return m.group(1).replaceAll("-", "");
        }
        return value;
    }

    protected String reduceUriToOrcidId(String value) {
        if (!StringUtils.isAllEmpty(value)) {
            var m = pattern.matcher(value);
            if (m.find()) {
                String s = m.group(1).replaceAll("-", "");
                return StringUtils
                    .leftPad(s, 16, '0')
                    .replaceAll("(\\d\\d\\d\\d)", "$1-")
                    .replaceAll("-$", "");
            }
        }
        return value;
    }

    public abstract String getScheme();

    public String getValue() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getScheme(), getValue());
    }
}
