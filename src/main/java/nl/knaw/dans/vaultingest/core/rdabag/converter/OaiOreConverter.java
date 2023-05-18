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

import nl.knaw.dans.vaultingest.core.rdabag.mappers.AlternativeTitle;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.Author;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.OtherIds;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.Title;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.OreResourceMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        namespaces.put("cit", citation);
        namespaces.put("dcterms", dcterms);
        namespaces.put("datacite", datacite);
        namespaces.put("ore", ore);
        namespaces.put("dc", dc);
        namespaces.put("foaf", foaf);
        namespaces.put("schema", schema);

        var model = ModelFactory.createDefaultModel();

        for (var namespace : namespaces.entrySet()) {
            model.setNsPrefix(namespace.getKey(), namespace.getValue());
        }

        var resourceMap = createResourceMap(model);
        var resource = createAggregation(model);

        model.add(Title.toTitle(resource, deposit.getTitle()));
        model.add(AlternativeTitle.toAlternativeTitle(resource, deposit.getAlternativeTitles()));
        model.add(OtherIds.toOtherIds(resource, deposit.getOtherIds()));
        model.add(Author.toAuthors(resource, deposit.getAuthors()));

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

    Resource createAggregatedResource(Model model, String id) {
        var resource = model.createResource("urn:uuid:" + id);

        var type = model.createStatement(resource, RDF.type,
            model.createResource("http://www.openarchives.org/ore/terms/AggregatedResource"));

        var name = model.createStatement(resource,
            model.createProperty("http://schema.org/name"),
            model.createLiteral("This is the name for id " + id)
        );

        model.add(type);
        model.add(name);

        return resource;
    }

    Resource createAggregation(Model model) {
        var resource = model.createResource("urn:nbn:nl:ui-13-jc-8o2t");
        var type1 = model.createStatement(resource, RDF.type,
            model.createResource("http://www.openarchives.org/ore/terms/Aggregation"));

        model.add(type1);

        var file1 = UUID.randomUUID().toString();
        var file2 = UUID.randomUUID().toString();
        var file3 = UUID.randomUUID().toString();

        var r1 = createAggregatedResource(model, file1);
        var r2 = createAggregatedResource(model, file2);
        var r3 = createAggregatedResource(model, file3);

        model.add(model.createStatement(resource, model.createProperty(ore, "aggregates"), r1));
        model.add(model.createStatement(resource, model.createProperty(ore, "aggregates"), r2));
        model.add(model.createStatement(resource, model.createProperty(ore, "aggregates"), r3));

        return resource;
    }
//
//    // this should go into one of those special static classes
//    void addCreators(Model model, Resource resource, Deposit deposit) {
//        for (var author : deposit.getAuthors()) {
//            var authorResource = model.createResource();
//
//            model.add(model.createStatement(authorResource,
//                model.createProperty(citation, "authorName"),
//                model.createLiteral(formatName(author))));
//
//            model.add(model.createStatement(authorResource,
//                model.createProperty(citation, "authorAffiliation"),
//                model.createLiteral(author.getAffiliation())));
//
//            var authorMap = new HashMap<String, String>();
//
//            if (author.getOrcid() != null) {
//                authorMap.put("authorIdentifierScheme", "ORCID");
//                authorMap.put("authorIdentifier", author.getOrcid());
//            }
//            else if (author.getIsni() != null) {
//                authorMap.put("authorIdentifierScheme", "ISNI");
//                authorMap.put("authorIdentifier", author.getIsni());
//            }
//            else if (author.getDai() != null) {
//                authorMap.put("authorIdentifierScheme", "DAI");
//                authorMap.put("authorIdentifier", author.getDai());
//            }
//
//            model.add(model.createStatement(authorResource,
//                model.createProperty(datacite, "AgentIdentifier"),
//                model.createLiteral(authorMap.get("authorIdentifier"))));
//
//            model.add(model.createStatement(authorResource,
//                model.createProperty(datacite, "AgentIdentifierScheme"),
//                model.createLiteral(authorMap.get("authorIdentifierScheme"))));
//
//            model.add(resource, model.createProperty(dcterms, "author"), authorResource);
//        }
//    }

    String formatName(DatasetAuthor author) {
        return String.join(" ", List.of(
                Optional.ofNullable(author.getInitials()).orElse(""),
                Optional.ofNullable(author.getInsertions()).orElse(""),
                Optional.ofNullable(author.getSurname()).orElse("")
            ))
            .trim().replaceAll("\\s+", " ");
    }
}
