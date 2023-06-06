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
package nl.knaw.dans.vaultingest.core.rdabag.mappers;

import nl.knaw.dans.vaultingest.core.domain.metadata.Keyword;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCitation;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Keywords {

    public static List<Statement> toKeywords(Resource resource, Collection<Keyword> keywords) {
        if (keywords == null) {
            return List.of();
        }

        var model = resource.getModel();
        var result = new ArrayList<Statement>();

        for (var keyword: keywords) {
            var element = model.createResource();
            element.addProperty(DVCitation.keywordValue, keyword.getText());

            if (keyword.getVocabulary() != null) {
                element.addProperty(DVCitation.keywordVocabulary, keyword.getVocabulary());
            }
            if (keyword.getVocabularyUri() != null) {
                element.addProperty(DVCitation.keywordVocabularyURI, keyword.getVocabularyUri());
            }

            result.add(model.createStatement(
                resource,
                DVCitation.keyword,
                element
            ));
        }

        return result;
    }
}
