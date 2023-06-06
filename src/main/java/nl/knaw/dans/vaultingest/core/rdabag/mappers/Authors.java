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

import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetRelation;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.Datacite;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Authors {

    public static List<Statement> toAuthors(Resource resource, Collection<DatasetRelation> authors) {
        if (authors == null) {
            return List.of();
        }

        var model = resource.getModel();
        var result = new ArrayList<Statement>();

        for (var author: authors) {
            var authorElement = model.createResource();
            authorElement.addProperty(DVCitation.authorName, author.getDisplayName());

            if (author.getAffiliation() != null) {
                authorElement.addProperty(DVCitation.authorAffiliation, author.getAffiliation());
            }

            if (author.getIdentifierScheme() != null) {
                authorElement.addProperty(Datacite.agentIdentifierScheme, author.getIdentifierScheme());
            }

            if (author.getIdentifier() != null) {
                authorElement.addProperty(Datacite.agentIdentifier, author.getIdentifier());
            }

            result.add(model.createStatement(
                resource,
                DVCitation.author,
                authorElement
            ));
        }

        return result;
    }
}
