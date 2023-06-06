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

import nl.knaw.dans.vaultingest.core.domain.metadata.Publication;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.Datacite;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Publications {

    public static List<Statement> toPublications(Resource resource, Collection<Publication> publications) {
        if (publications == null) {
            return List.of();
        }

        var model = resource.getModel();
        var result = new ArrayList<Statement>();

        for (var publication: publications) {
            var element = model.createResource();
            element.addProperty(Datacite.resourceIdentifier, publication.getIdNumber());
            element.addProperty(Datacite.resourceIdentifierScheme, publication.getIdType());

            result.add(model.createStatement(
                resource,
                DCTerms.isReferencedBy,
                element
            ));
        }

        return result;
    }
}
