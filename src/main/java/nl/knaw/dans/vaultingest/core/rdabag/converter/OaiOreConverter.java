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
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DansRel;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.Datacite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SchemaDO;

import java.util.HashMap;

public class OaiOreConverter {

    private final String ore = "http://www.openarchives.org/ore/terms/";
    private final String dcterms = "http://purl.org/dc/terms/";
    private final String citation = "https://dataverse.org/schema/citation/";
    private final String foaf = "http://xmlns.com/foaf/0.1/";
    private final String dc = "http://purl.org/dc/elements/1.1/";
    private final String schema = "http://schema.org/";
    private final String datacite = "http://purl.org/spar/datacite/";

    public OreResourceMap convert(Deposit deposit) {
        var namespaces = new HashMap<String, String>();
        namespaces.put("cit", DVCitation.NS);
        namespaces.put("dcterms", DCTerms.NS);
        namespaces.put("datacite", Datacite.NS);
        namespaces.put("ore", ore);
        namespaces.put("dc", DC_11.NS);
        namespaces.put("foaf", FOAF.NS);
        namespaces.put("schema", SchemaDO.NS);
        namespaces.put("dansREL", DansRel.NS);

        var model = ModelFactory.createDefaultModel();

        for (var namespace : namespaces.entrySet()) {
            model.setNsPrefix(namespace.getKey(), namespace.getValue());
        }

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

        model.add(model.createStatement(resourceMap, model.createProperty(ore, "describes"), resource));

        return new OreResourceMap(model, namespaces);
    }

    Resource createResourceMap(Model model) {
        var resourceMap = model.createResource("urn:uuid:95ac8641-407c-4f1b-8d83-a7e659ca409a");

        var resourceMapType = model.createStatement(resourceMap, RDF.type,
            model.createResource("http://www.openarchives.org/ore/terms/ResourceMap"));

        model.add(resourceMapType);
        return resourceMap;
    }

    Resource createAggregatedResource(Model model, DepositFile depositFile) {
        var resource = model.createResource("urn:uuid:" + depositFile.getId());

        var type = model.createStatement(resource, RDF.type,
            model.createResource("http://www.openarchives.org/ore/terms/AggregatedResource"));

        var name = model.createStatement(resource, SchemaDO.name, depositFile.getPath().toString());

        // TODO add description, access rights and dvcore:restricted, and checksum
        model.add(type);
        model.add(name);

        return resource;
    }

    Resource createAggregation(Deposit deposit, Model model) {
        var resource = model.createResource(deposit.getNbn());
        var type1 = model.createStatement(resource, RDF.type,
            model.createResource("http://www.openarchives.org/ore/terms/Aggregation"));

        model.add(type1);

        for (var file : deposit.getPayloadFiles()) {
            var fileResource = createAggregatedResource(model, file);
            model.add(model.createStatement(resource, model.createProperty(ore, "aggregates"), fileResource));
        }

        return resource;
    }
}
