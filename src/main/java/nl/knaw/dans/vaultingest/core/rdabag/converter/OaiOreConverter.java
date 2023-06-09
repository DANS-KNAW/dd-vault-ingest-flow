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
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCore;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.ORE;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SchemaDO;

import java.time.OffsetDateTime;

public class OaiOreConverter {

    public OreResourceMap convert(Deposit deposit) {
        var model = ModelFactory.createDefaultModel();

        var resourceMap = createResourceMap(deposit, model);
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
        model.add(GrantNumbers.toGrantNumbers(resource, deposit.getGrantNumbers()));
        model.add(Distributors.toDistributors(resource, deposit.getDistributors()));

        DistributionDates.toDistributionDate(resource, deposit.getDistributionDate())
            .ifPresent(model::add);

        model.add(DatesOfCollections.toDatesOfCollection(resource, deposit.getCollectionDates()));
        model.add(Series.toSeries(resource, deposit.getSeries()));
        model.add(DataSources.toDataSources(resource, deposit.getSources()));

        model.add(DansRightsHolders.toDansRightsHolders(resource, deposit.getRightsHolder()));
        model.add(DansPersonalDataPresent.toDansPersonalDataPresent(resource, deposit.isPersonalDataPresent()));
        model.add(DansMetadataLanguages.toLanguages(resource, deposit.getMetadataLanguages()));

        model.add(model.createStatement(
            resourceMap,
            ORE.describes,
            resource
        ));

        return new OreResourceMap(model);
    }

    Resource createResourceMap(Deposit deposit, Model model) {
        var resourceMap = model.createResource("urn:uuid:" + deposit.getId());
        var resourceMapType = model.createStatement(resourceMap, RDF.type, ORE.ResourceMap);

        model.add(resourceMapType);
        model.add(model.createStatement(
            resourceMap,
            DCTerms.modified,
            OffsetDateTime.now().toString()
        ));

        var creator = model.createResource();
        model.add(model.createStatement(
            creator,
            FOAF.name,
            "DANS Vault Service"
        ));

        model.add(model.createStatement(
            resourceMap,
            DCTerms.creator,
            creator
        ));

        return resourceMap;
    }

    Resource createAggregatedResource(Model model, DepositFile depositFile) {
        var resource = model.createResource("urn:uuid:" + depositFile.getId());

        // TODO add access rights and checksum
        model.add(model.createStatement(resource, RDF.type, ORE.AggregatedResource));
        model.add(model.createStatement(resource, SchemaDO.name, depositFile.getPath().toString()));

        // FIL002A, FIL002B, FIL003, FIL004
        var descriptionText = depositFile.getDescription();

        if (descriptionText != null) {
            var description = model.createStatement(resource, SchemaDO.description, depositFile.getDescription());
            model.add(description);
        }

        // FIL002
        var directoryLabel = depositFile.getDirectoryLabel();

        if (directoryLabel != null) {
            model.add(model.createStatement(resource, DVCore.directoryLabel, directoryLabel.toString()));
        }

        // FIL005, FIL006, FIL007
        model.add(model.createStatement(resource, DVCore.restricted, Boolean.toString(depositFile.isRestricted())));

        return resource;
    }

    Resource createAggregation(Deposit deposit, Model model) {
        var resource = model.createResource(deposit.getNbn());
        var type = model.createStatement(resource, RDF.type, ORE.Aggregation);

        model.add(type);

        if (deposit.getPayloadFiles() != null) {
            for (var file : deposit.getPayloadFiles()) {
                var fileResource = createAggregatedResource(model, file);

                model.add(model.createStatement(
                    resource,
                    ORE.aggregates,
                    fileResource
                ));
            }
        }

        return resource;
    }
}
