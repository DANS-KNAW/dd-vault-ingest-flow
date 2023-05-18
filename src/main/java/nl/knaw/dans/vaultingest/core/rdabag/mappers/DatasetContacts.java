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

import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetContact;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCitation;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.Optional;

public class DatasetContacts {

    public static Optional<Statement> toDatasetContact(Resource resource, DatasetContact datasetContact) {
        if (datasetContact == null) {
            return Optional.empty();
        }

        var model = resource.getModel();
        var authorElement = model.createResource();

        authorElement.addProperty(DVCitation.datasetContactName, datasetContact.getName());
        authorElement.addProperty(DVCitation.datasetContactEmail, datasetContact.getEmail());

        if (datasetContact.getAffiliation() != null) {
            authorElement.addProperty(DVCitation.datasetContactAffiliation, datasetContact.getAffiliation());
        }

        return Optional.of(model.createStatement(
            resource,
            DVCitation.datasetContact,
            authorElement
        ));
    }
}