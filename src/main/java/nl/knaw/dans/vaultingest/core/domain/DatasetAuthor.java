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
package nl.knaw.dans.vaultingest.core.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import nl.knaw.dans.vaultingest.core.domain.ids.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Builder
@Setter(AccessLevel.NONE)
public class DatasetAuthor implements DatasetRelation {
    private String name;
    private String titles;
    private String initials;
    private String insertions;
    private String surname;
    private DAI dai;
    private ISNI isni;
    private ORCID orcid;
    private String role;
    private String organization;
    private String affiliation;

    public String getDisplayName() {
        // initials + insertions + surname
        return Stream.of(
                this.getInitials(),
                this.getInsertions(),
                this.getSurname()
            ).filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" "));
    }


    public String getIdentifierScheme() {
        var identifier = this.getIdentifierObject();

        if (identifier == null) {
            return null;
        }

        return identifier.getScheme();
    }

    public String getIdentifier() {
        var identifier = this.getIdentifierObject();

        if (identifier == null) {
            return null;
        }

        return identifier.getValue();
    }

    private BaseId getIdentifierObject() {
        var schemes = new BaseId[] {
            this.orcid,
            this.isni,
            this.dai,
        };

        // return first match
        for (var scheme : schemes) {
            if (scheme != null) {
                return scheme;
            }
        }

        return null;
    }
}
