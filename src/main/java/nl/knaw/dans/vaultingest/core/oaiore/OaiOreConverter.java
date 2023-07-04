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
package nl.knaw.dans.vaultingest.core.oaiore;

import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.deposit.DepositFile;
import nl.knaw.dans.vaultingest.core.mappings.*;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.ORE;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SchemaDO;

import java.time.OffsetDateTime;

public class OaiOreConverter {

    public Model convert(Deposit deposit) {
        var model = ModelFactory.createDefaultModel();

        var resourceMap = createResourceMap(deposit, model);
        var resource = createAggregation(deposit, model);

        model.add(Titles.toRDF(resource, deposit));
        AlternativeTitles.toRDF(resource, deposit)
                .ifPresent(model::add);

        model.add(OtherIds.toRDF(resource, deposit));
        model.add(Authors.toRDF(resource, deposit));

        model.add(Descriptions.toRDF(resource, deposit));
        model.add(Subjects.toRDF(resource, deposit));
        model.add(Keywords.toRDF(resource, deposit));
        model.add(Publications.toRDF(resource, deposit));
        model.add(Languages.toRDF(resource, deposit));

        ProductionDate.toRDF(resource, deposit)
                .ifPresent(model::add);

        model.add(Contributors.toRDF(resource, deposit));
        model.add(GrantNumbers.toRDF(resource, deposit));
        model.add(Distributors.toRDF(resource, deposit));

        DistributionDate.toRDF(resource, deposit)
                .ifPresent(model::add);

        model.add(CollectionDates.toRDF(resource, deposit));
        model.add(Sources.toRDF(resource, deposit));

        model.add(RightsHolders.toRDF(resource, deposit));
        model.add(PersonalData.toRDF(resource, deposit));
        model.add(Languages.toRDF(resource, deposit));

        model.add(Audiences.toRDF(resource, deposit));
        model.add(InCollection.toRDF(resource, deposit));
        model.add(DansRelations.toRDF(resource, deposit));

        model.add(TemporalCoverage.toRDF(resource, deposit));
        model.add(SpatialCoverage.toRDF(resource, deposit));

        model.add(VaultMetadata.toRDF(resource, deposit));
        model.add(MetadataLanguages.toRDF(resource, deposit));
        License.toRDF(resource, deposit).ifPresent(model::add);

        model.add(Terms.toRDF(resource, deposit));

        model.add(model.createStatement(
                resourceMap,
                ORE.describes,
                resource
        ));

        return model;
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

        model.add(model.createStatement(resource, RDF.type, ORE.AggregatedResource));
        model.add(model.createStatement(resource, SchemaDO.name, depositFile.getPath().toString()));
        model.add(DataFile.toRDF(resource, depositFile));

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
