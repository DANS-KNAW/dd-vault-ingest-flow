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

import nl.knaw.dans.vaultingest.core.domain.metadata.OtherId;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class OtherIds {

    public static List<Statement> toOtherIds(Resource resource, Collection<OtherId> titles) {
        if (titles == null) {
            return List.of();
        }

        var model = resource.getModel();

        return titles.stream()
            .map(id -> {
                var namespace = OaiOreNamespaces.CITATION;
                var otherId = model.createResource();

                otherId.addProperty(model.createProperty(namespace, "otherIdValue"), id.getValue());

                if (id.getAgency() != null) {
                    otherId.addProperty(model.createProperty(namespace, "otherIdAgency"), id.getAgency());
                }

                return model.createStatement(
                    resource,
                    model.createProperty(namespace, "otherId"),
                    otherId
                );
            })
            .collect(Collectors.toList());
    }
}
