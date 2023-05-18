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

import nl.knaw.dans.vaultingest.core.domain.metadata.Description;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.w3c.dom.Document;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Descriptions {
    public static List<Description> getDescriptions(Document document) {
        // CIT009, profile / description
        var profileDescriptions = XPathEvaluator.strings(document,
            "/ddm:DDM/ddm:profile/dc:description",
            "/ddm:DDM/ddm:profile/dcterms:description"
        ).map(value -> Description.builder()
            .value(value.trim())
            .build()
        );

        // TODO CIT010, alternative titles and non-first title?

        // CIT011, dcmiMetadata / [tags]
        var dcmiDescriptions = XPathEvaluator.nodes(document,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:date",
                "/ddm:DDM/ddm:dcmiMetadata/dc:date",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateAccepted",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateCopyrighted",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateSubmitted",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:modified",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:issued",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:valid",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:coverage")
            .map(node -> Description.builder()
                .type(node.getLocalName())
                .value(node.getTextContent().trim())
                .build()
            );

        // CIT012, dcmiMetadata / description
        var dcmiDescription = XPathEvaluator.strings(document,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:description")
            .map(value -> Description.builder()
                .value(value.trim())
                .build()
            );

        var streams = Stream.concat(profileDescriptions,
            Stream.concat(dcmiDescriptions, dcmiDescription)
        );

        return streams.collect(Collectors.toList());
    }
}
