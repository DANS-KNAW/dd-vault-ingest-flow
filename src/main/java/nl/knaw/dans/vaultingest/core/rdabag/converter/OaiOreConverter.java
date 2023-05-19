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
package nl.knaw.dans.vaultingest.core.rdabag.converter;

import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.OreResourceMap;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.*;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.ORE;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SchemaDO;

public class OaiOreConverter {

    public OreResourceMap convert(Deposit deposit) {
        var model = ModelFactory.createDefaultModel();

        var resourceMap = createResourceMap(model);
        var resource = createAggregation(deposit, model);

        model.add(Title.toTitle(resource, deposit.getTitle()));
        model.add(AlternativeTitles.toAlternativeTitle(resource, deposit.getAlternativeTitles()));
        model.add(OtherIds.toOtherIds(resource, deposit.getOtherIds()));
        model.add(Authors.toAuthors(resource, deposit.getAuthors()));

        DatasetContacts.toDatasetContact(resource, deposit.getContact())
            .ifPresent(model::add);

        model.add(Descriptions.toDescriptions(resource, deposit.getDescriptions()));
        model.add(Subjects.toSubjects(resource, deposit.getSubjects()));
        model.add(Keywords.toKeywords(resource, deposit.getKeywords()));
        model.add(Publications.toPublications(resource, deposit.getPublications()));
        model.add(Languages.toLanguages(resource, deposit.getLanguages()));

        ProductionDates.toProductionDate(resource, deposit.getProductionDate())
            .ifPresent(model::add);

        model.add(Contributors.toContributors(resource, deposit.getContributors()));

        model.add(model.createStatement(
            resourceMap,
            ORE.describes,
            resource
        ));


        return new OreResourceMap(model);
    }

    Resource createResourceMap(Model model) {
        var resourceMap = model.createResource("urn:uuid:95ac8641-407c-4f1b-8d83-a7e659ca409a");
        var resourceMapType = model.createStatement(resourceMap, RDF.type, ORE.ResourceMap);

        model.add(resourceMapType);
        return resourceMap;
    }

    Resource createAggregatedResource(Model model, DepositFile depositFile) {
        var resource = model.createResource("urn:uuid:" + depositFile.getId());

        var type = model.createStatement(resource, RDF.type, ORE.AggregatedResource);
        var name = model.createStatement(resource, SchemaDO.name, depositFile.getPath().toString());

        // TODO add description, access rights and dvcore:restricted, and checksum
        model.add(type);
        model.add(name);

        return resource;
    }

    Resource createAggregation(Deposit deposit, Model model) {
        var resource = model.createResource(deposit.getNbn());
        var type = model.createStatement(resource, RDF.type, ORE.Aggregation);

        model.add(type);

        for (var file : deposit.getPayloadFiles()) {
            var fileResource = createAggregatedResource(model, file);

            model.add(model.createStatement(
                resource,
                ORE.aggregates,
                fileResource
            ));
        }

        return resource;
    }
}
