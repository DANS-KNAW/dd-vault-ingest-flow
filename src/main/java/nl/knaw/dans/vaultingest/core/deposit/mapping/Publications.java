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

import nl.knaw.dans.vaultingest.core.domain.metadata.Publication;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.w3c.dom.Document;

import java.util.List;
import java.util.stream.Collectors;

public class Publications {

    public static List<Publication> getPublications(Document document) {
        // CIT017
        // TODO the spec says dc:identifier, but example uses dcterms:identifier
        return XPathEvaluator.nodes(document, "/ddm:DDM/ddm:dcmiMetadata/dcterms:identifier[" +
                "@xsi:type = 'id-type:ISSN' or @xsi:type = 'id-type:ISBN'" +
                "]")
            .map(node -> {
                var idType = node.getAttributes()
                    .getNamedItem("xsi:type").getTextContent()
                    .trim()
                    .replaceAll("id-type:", "");

                return Publication.builder()
                    .idType(idType)
                    .idNumber(node.getTextContent().trim())
                    .build();
            })
            .collect(Collectors.toList());
    }
}
