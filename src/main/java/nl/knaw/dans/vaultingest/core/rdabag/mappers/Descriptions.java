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

import nl.knaw.dans.vaultingest.core.domain.metadata.Description;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCitation;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Descriptions {

    public static List<Statement> toDescriptions(Resource resource, Collection<Description> descriptions) {
        if (descriptions == null) {
            return List.of();
        }

        var model = resource.getModel();
        var result = new ArrayList<Statement>();

        for (var description: descriptions) {
            var element = model.createResource();
            element.addProperty(DVCitation.dsDescriptionValue, description.getValue());

            if (description.getDate() != null) {
                element.addProperty(DVCitation.dsDescriptionDate, description.getDate());
            }

            result.add(model.createStatement(
                resource,
                DVCitation.dsDescription,
                element
            ));
        }

        return result;
    }
}
