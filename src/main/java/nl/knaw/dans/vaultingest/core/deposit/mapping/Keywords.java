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

import nl.knaw.dans.vaultingest.core.domain.metadata.Keyword;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Keywords {

    final public static String SCHEME_PAN = "PAN thesaurus ideaaltypes";
    final public static String SCHEME_URI_PAN = "https://data.cultureelerfgoed.nl/term/id/pan/PAN";

    final public static String SCHEME_AAT = "Art and Architecture Thesaurus";
    final public static String SCHEME_URI_AAT = "http://vocab.getty.edu/aat/";

    private static final Map<String, String> narcisToSubject = new HashMap<>();

    public static List<Keyword> getKeywords(Document document) {
        var results = new ArrayList<Keyword>();
        results.addAll(getKeywordsWithoutScheme(document));
        results.addAll(getPanAndAatKeywords(document));
        results.addAll(getLanguageKeywords(document));

        return results;
    }

    static List<Keyword> getKeywordsWithoutScheme(Document document) {
        // CIT014
        return XPathEvaluator.nodes(document,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:subject[not(@schemeURI) and not(@subjectScheme)]",
                "/ddm:DDM/ddm:dcmiMetadata/dc:subject[not(@schemeURI) and not(@subjectScheme)]")
            .map(node -> Keyword.builder().text(node.getTextContent().trim()).build())
            .collect(Collectors.toList());
    }

    static List<Keyword> getPanAndAatKeywords(Document document) {
        var expr = "/ddm:DDM/ddm:dcmiMetadata/ddm:subject[" +
            String.format("(@schemeURI = '%s' and @subjectScheme = '%s')", SCHEME_URI_PAN, SCHEME_PAN) +
            " or " +
            String.format("(@schemeURI = '%s' and @subjectScheme = '%s')", SCHEME_URI_AAT, SCHEME_AAT) +
            "]";

        // CIT014
        return XPathEvaluator.nodes(document, expr)
            .map(node -> Keyword.builder()
                .text(node.getTextContent().trim())
                .vocabulary(node.getAttributes().getNamedItem("subjectScheme").getTextContent())
                .vocabularyUri(node.getAttributes().getNamedItem("schemeURI").getTextContent())
                .build())
            .collect(Collectors.toList());
    }

    static List<Keyword> getLanguageKeywords(Document document) {
        return XPathEvaluator.strings(document,
            "/ddm:DDM/ddm:dcmiMetadata/dcterms:language"
        ).map(value -> Keyword.builder()
            .text(value.trim())
            .build()
        ).collect(Collectors.toList());
    }
}
