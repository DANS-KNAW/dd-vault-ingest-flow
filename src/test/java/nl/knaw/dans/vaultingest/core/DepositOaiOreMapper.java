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
package nl.knaw.dans.vaultingest.core;

import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.OreResourceMap;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.RDF;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.*;

// TODO convert this to a test class for playing with apache jena
public class DepositOaiOreMapper {
    private final Map<String, String> namespaces = new HashMap<>();
    private final Map<String, String> namespaceChildren = new HashMap<>();

    private final String ore = "http://www.openarchives.org/ore/terms/";
    private final String dcterms = "http://purl.org/dc/terms/";
    private final String citation = "https://dataverse.org/schema/citation/";
    private final String foaf = "http://xmlns.com/foaf/0.1/";
    private final String dc = "http://purl.org/dc/elements/1.1/";
    private final String schema = "http://schema.org/";

    public DepositOaiOreMapper() {
        // This should be some kind of configuration
        namespaces.put("author", "http://purl.org/dc/terms/creator");
        namespaces.put("authorIdentifier", "http://purl.org/spar/datacite/AgentIdentifier");
        namespaces.put("authorIdentifierScheme", "http://purl.org/spar/datacite/AgentIdentifierScheme");
        namespaces.put("citation", "https://dataverse.org/schema/citation/");
        namespaces.put("dansDataVaultMetadata", "https://dar.dans.knaw.nl/schema/dansDataVaultMetadata#");
        namespaces.put("dansRelationMetadata", "https://dar.dans.knaw.nl/schema/dansRelationMetadata#");
        namespaces.put("dansRights", "https://dar.dans.knaw.nl/schema/dansRights#");
        namespaces.put("dcterms", "http://purl.org/dc/terms/");
        namespaces.put("dvcore", "https://dataverse.org/schema/core#");
        namespaces.put("lang", "@language");
        namespaces.put("ore", "http://www.openarchives.org/ore/terms/");
        namespaces.put("schema", "http://schema.org/");
        namespaces.put("subject", "http://purl.org/dc/terms/subject");
        namespaces.put("termName", "https://schema.org/name");
        namespaces.put("title", "http://purl.org/dc/terms/title");
        namespaces.put("value", "@value");
        namespaces.put("vocabularyName", "https://dataverse.org/schema/vocabularyName");
        namespaces.put("vocabularyUri", "https://dataverse.org/schema/vocabularyUri");

        namespaceChildren.put("dansRightsHolder", "dansRights");
        namespaceChildren.put("dansPersonalDataPresent", "dansRights");
        namespaceChildren.put("authorName", "citation");
        namespaceChildren.put("authorAffiliation", "citation");

    }

    public OreResourceMap mapDepositToOaiOre2(Deposit deposit) {
        var namespaces = new HashMap<String, String>();
        namespaces.put("cit", citation);
        namespaces.put("dct", dcterms);
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

        model.add(model.createStatement(resourceMap,
                model.createProperty(dcterms, "modified"),
                model.createLiteral("2008-10-01T18:30:02Z")));

        var hasPart1 = model.createStatement(resource,
                model.createProperty(schema, "hasPart"),
                model.createLiteral("urn:uuid:123")
        );

        var hasPart2 = model.createStatement(resource,
                model.createProperty(schema, "hasPart"),
                model.createLiteral("urn:uuid:124")
        );

        model.add(hasPart1);
        model.add(hasPart2);

        model.add(model.createStatement(
                resource,
                model.createProperty(dcterms, "title"),
                model.createLiteral("This is the title")
        ));

        var otherId = model.createResource();
        otherId.addProperty(model.createProperty(citation, "otherIdAgency"), "Test prefix");
        otherId.addProperty(model.createProperty(citation, "otherIdValue"), "1234");

        model.add(resource, model.createProperty(citation, "otherId"), otherId);


        model.add(model.createStatement(resourceMap, model.createProperty(ore, "describes"), resource));

        return new OreResourceMap(model);
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

    public OreResourceMap mapDepositToOaiOre(Deposit deposit) {
        var namespaces = new HashMap<String, String>();
        namespaces.put("cit", citation);
        namespaces.put("dct", dcterms);
        namespaces.put("ore", ore);
        namespaces.put("dc", dc);
        namespaces.put("foaf", foaf);

        var model = ModelFactory.createDefaultModel();

        for (var namespace : namespaces.entrySet()) {
            model.setNsPrefix(namespace.getKey(), namespace.getValue());
        }

        try {
            model.read(new ByteArrayInputStream(rdfXmlExample().getBytes()), null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new OreResourceMap(model);
    }

    void frame() {
        // a "frame" is a specific graph layout that is applied to output data
        // It can be used to filter the output data.
        // In this example, we show how to output only the resources of a givn rdf:type

        Model m = ModelFactory.createDefaultModel();
        String ns = "http://schema.org/";
        Resource person = m.createResource(ns + "Person");
        Resource s = m.createResource();
        m.add(s, m.createProperty(ns + "name"), "Jane Doe");
        m.add(s, m.createProperty(ns + "url"), "http://www.janedoe.com");
        m.add(s, m.createProperty(ns + "jobTitle"), "Professor");
        m.add(s, RDF.type, person);
        s = m.createResource();
        m.add(s, m.createProperty(ns + "name"), "Gado Salamatou");
        m.add(s, m.createProperty(ns + "url"), "http://www.salamatou.com");
        m.add(s, RDF.type, person);
        s = m.createResource();
        m.add(s, m.createProperty(ns + "name"), "Not a person");
        m.add(s, RDF.type, m.createResource(ns + "Event"));

        DatasetGraph g = DatasetFactory.wrap(m).asDatasetGraph();
        JsonLDWriteContext ctx = new JsonLDWriteContext();

        // only output the persons using a frame

        String frame = "{\"@type\" : \"http://schema.org/Person\"}";
        ctx.setFrame(frame);
        System.out.println("\n--- Using frame to select resources to be output: only output persons ---");
        write(g, RDFFormat.JSONLD10_FRAME_PRETTY, ctx);
    }

    void write(OutputStream out, DatasetGraph g, RDFFormat f, Context ctx) {
        RDFWriter w =
                RDFWriter.create()
                        .format(f)
                        .source(g)
                        .context(ctx)
                        .build();
        w.output(out);
    }

    private void write(DatasetGraph g, RDFFormat f, Context ctx) {
        write(System.out, g, f, ctx);
    }


    String rdfXmlExample() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n" +
                "    xmlns:ore=\"http://www.openarchives.org/ore/terms/\"\n" +
                "    xmlns:dcterms=\"http://purl.org/dc/terms/\"\n" +
                "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                "    xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" >\n" +
                "\n" +
                "    <!-- About the Aggregation for the ArXiv document -->\n" +
                "\n" +
                "    <rdf:Description rdf:about=\"http://arxiv.org/aggregation/astro-ph/0601007\">\n" +
                "        <!-- The Resource is an ORE Aggregation  -->\n" +
                "        <rdf:type rdf:resource=\"http://www.openarchives.org/ore/terms/Aggregation\"/>\n" +
                "        <!-- The Aggregation aggregates ... -->\n" +
                "        <ore:aggregates rdf:resource=\"http://arxiv.org/abs/astro-ph/0601007\"/>\n" +
                "        <ore:aggregates rdf:resource=\"http://arxiv.org/ps/astro-ph/0601007\"/>\n" +
                "        <ore:aggregates rdf:resource=\"http://arxiv.org/pdf/astro-ph/0601007\"/>\n" +
                "        <!-- Metadata about the Aggregation: title and authors -->\n" +
                "        <dc:title>Parametrization of K-essence and Its Kinetic Term</dc:title>\n" +
                "        <dcterms:creator rdf:parseType=\"Resource\">\n" +
                "            <foaf:name>Hui Li</foaf:name>\n" +
                "            <foaf:mbox rdf:resource=\"mailto:lihui@somewhere.cn\"/>\n" +
                "        </dcterms:creator>\n" +
                "        <dcterms:creator rdf:parseType=\"Resource\">\n" +
                "            <foaf:name>Zong-Kuan Guo</foaf:name>\n" +
                "        </dcterms:creator>\n" +
                "        <dcterms:creator rdf:parseType=\"Resource\">\n" +
                "            <foaf:name>Yuan-Zhong Zhang</foaf:name>\n" +
                "        </dcterms:creator>\n" +
                "    </rdf:Description>\n" +
                "    \n" +
                "    <!-- About the Resource Map (this RDF/XML document) that describes the Aggregation -->\n" +
                "    \n" +
                "    <rdf:Description rdf:about=\"http://arxiv.org/rem/atom/astro-ph/0601007\">\n" +
                "        <!-- The Resource is an ORE Resource Map  -->\n" +
                "        <rdf:type rdf:resource=\"http://www.openarchives.org/ore/terms/ResourceMap\"/>\n" +
                "        <!-- The Resource Map describes a specific Aggregation   -->\n" +
                "        <ore:describes rdf:resource=\"http://arxiv.org/aggregation/astro-ph/0601007\"/>\n" +
                "        <!-- Metadata about the Resource Map: datetimes, rights, and author -->\n" +
                "        <dcterms:modified>2008-10-03T07:30:34Z</dcterms:modified>\n" +
                "        <dcterms:created>2008-10-01T18:30:02Z</dcterms:created>\n" +
                "        <dc:rights>This Resource Map is available under the Creative Commons Attribution-Noncommercial Generic license</dc:rights>\n" +
                "        <dcterms:rights rdf:resource=\"http://creativecommons.org/licenses/by-nc/2.5/rdf\"/>\n" +
                "        <dcterms:creator rdf:parseType=\"Resource\">\n" +
                "            <foaf:page rdf:resource=\"http://arxiv.org\"/>\n" +
                "            <foaf:name>arXiv.org e-Print Repository</foaf:name>\n" +
                "        </dcterms:creator>\n" +
                "    </rdf:Description>\n" +
                "    \n" +
                "    <!-- About the human start page that is part of the Aggregation -->\n" +
                "    \n" +
                "    <rdf:Description rdf:about=\"http://arxiv.org/abs/astro-ph/0601007\">\n" +
                "        <dc:format>text/html</dc:format>\n" +
                "        <dc:title>[astro-ph/0601007] Parametrization of K-essence and Its Kinetic Term</dc:title>\n" +
                "        <rdf:type>info:eu-repo/semantics/humanStartPage</rdf:type>\n" +
                "    </rdf:Description>\n" +
                "    \n" +
                "    <!-- About the PostScript resource that is part of the Aggregation -->\n" +
                "    \n" +
                "    <rdf:Description rdf:about=\"http://arxiv.org/ps/astro-ph/0601007\">\n" +
                "        <dc:format>application/postscript</dc:format>\n" +
                "        <dc:language>en</dc:language>\n" +
                "        <dc:title>Parametrization of K-essence and Its Kinetic Term</dc:title>\n" +
                "    </rdf:Description>\n" +
                "    \n" +
                "    <!-- About the PDF resource that is part of the Aggregation -->\n" +
                "    \n" +
                "    <rdf:Description rdf:about=\"http://arxiv.org/pdf/astro-ph/0601007\">\n" +
                "        <dc:format>application/pdf</dc:format>\n" +
                "        <dc:language>en</dc:language>\n" +
                "        <dc:title>Parametrization of K-essence and Its Kinetic Term</dc:title>\n" +
                "    </rdf:Description>\n" +
                "</rdf:RDF>\n";
    }

}
