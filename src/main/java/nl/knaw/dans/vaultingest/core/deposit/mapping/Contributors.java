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
package nl.knaw.dans.vaultingest.core.deposit.mapping;

import nl.knaw.dans.vaultingest.core.domain.metadata.Contributor;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.w3c.dom.Document;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Contributors extends Base {
    public static List<Contributor> getContributors(Document document) {
        // CIT020
        var authors = XPathEvaluator.nodes(document,
                "/ddm:DDM/ddm:dcmiMetadata/dcx-dai:contributorDetails/" +
                    "dcx-dai:author[dcx-dai:role != 'RightsHolder']")
            .map(item -> {
                var author = Author.parseAuthor(item);
                var name = author.getDisplayName();

                if (author.getOrganization() != null) {
                    name = name + " (" + author.getOrganization() + ")";
                }

                return Contributor.builder()
                    .type(author.getRole())
                    .name(name)
                    .build();
            });

        // CIT021
        var organizations = XPathEvaluator.nodes(document,
                "/ddm:DDM/ddm:dcmiMetadata/dcx-dai:contributorDetails/" +
                    "dcx-dai:organization[dcx-dai:role != 'RightsHolder' and dcx-dai:role != 'Funder']")
            .map(item -> {
                var role = getFirstValue(item, "dcx-dai:role");
                var name = getFirstValue(item, "dcx-dai:name");

                return Contributor.builder()
                    .type(role)
                    .name(name)
                    .build();
            });

        return Stream.concat(authors, organizations).collect(Collectors.toList());
    }
}
