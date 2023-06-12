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
package nl.knaw.dans.vaultingest.core.rdabag.converter.mappers;

import nl.knaw.dans.vaultingest.core.domain.metadata.SeriesElement;
import nl.knaw.dans.vaultingest.core.rdabag.converter.mappers.vocabulary.DVCitation;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Series {

    public static List<Statement> toSeries(Resource resource, Collection<SeriesElement> series) {
        if (series == null) {
            return List.of();
        }

        var model = resource.getModel();
        var result = new ArrayList<Statement>();

        for (var item : series) {
            var element = model.createResource();

            if (item.getName() != null) {
                element.addProperty(DVCitation.seriesName, item.getName());
            }

            if (item.getInformation() != null) {
                element.addProperty(DVCitation.seriesInformation, item.getInformation());
            }

            result.add(model.createStatement(
                resource,
                DVCitation.series,
                element
            ));
        }

        return result;
    }
}