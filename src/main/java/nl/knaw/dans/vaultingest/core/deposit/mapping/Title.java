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

import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.SchemaDO;
import org.w3c.dom.Document;

import java.util.List;
import java.util.stream.Collectors;

public class Title {
    public static String getTitle(Document ddm) {
        return XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:profile/dc:title")
            .map(String::trim)
            .findFirst()
            .orElse(null);
    }

    public static List<String> getAlternativeTitles(Document ddm) {
        return XPathEvaluator.strings(ddm,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:title",
                "/ddm:DDM/ddm:dcmiMetadata/dc:title",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:alternative")
            .map(String::trim)
            .collect(Collectors.toList());
    }
}
