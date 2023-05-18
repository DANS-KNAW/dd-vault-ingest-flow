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

import nl.knaw.dans.vaultingest.core.domain.metadata.OtherId;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

public class OtherIds {

    public static List<OtherId> getOtherIds(Document document, List<String> organizationIdentifiers) {
        var results = new ArrayList<OtherId>();

        // CIT003, data from bag
        organizationIdentifiers
            .stream()
            .filter(value -> {
                var parts = value.split(":", 2);
                return parts.length == 2 && StringUtils.isNotBlank(parts[0]) && StringUtils.isNotBlank(parts[1]);
            })
            .map(value -> {
                var parts = value.split(":", 2);
                return OtherId.builder()
                    .agency(parts[0])
                    .value(parts[1])
                    .build();
            })
            .findFirst()
            .ifPresent(results::add);

        // CIT004, data from ddm
        XPathEvaluator.strings(document,
                "/ddm:DDM/ddm:dcmiMetadata/ddm:identifier[not(@xsi:type)]",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:identifier[not(@xsi:type)]")
            .map(identifier -> OtherId.builder()
                .value(identifier)
                .build()
            )
            .forEach(results::add);

        return results;
    }
}
