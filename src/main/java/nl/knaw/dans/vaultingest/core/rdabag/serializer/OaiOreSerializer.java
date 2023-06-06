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
package nl.knaw.dans.vaultingest.core.rdabag.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.domain.OreResourceMap;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.ORE;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.riot.writer.JsonLD10Writer;
import org.apache.jena.sparql.util.Context;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class OaiOreSerializer {

    private final ObjectMapper objectMapper;

    public OaiOreSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serializeAsRdf(OreResourceMap resourceMap) {
        var model = resourceMap.getModel();
        var topLevelResources = new Resource[] {
            ORE.AggregatedResource,
            ORE.Aggregation,
            ORE.ResourceMap,
        };

        var properties = new HashMap<String, Object>();
        properties.put("prettyTypes", topLevelResources);
        properties.put("showXmlDeclaration", "true");

        var output = new ByteArrayOutputStream();

        RDFWriter.create()
            .format(RDFFormat.RDFXML_ABBREV)
            .set(SysRIOT.sysRdfWriterProperties, properties)
            .source(model)
            .output(output);

        return output.toString();
    }

    public String serializeAsJsonLd(OreResourceMap resourceMap) {
        var model = resourceMap.getModel();
        var context = new Context();
        var namespaces = namespacesAsJsonObject(resourceMap.getUsedNamespaces());
        var contextStr = "{ \"@context\": [\n" +
            "    \"https://w3id.org/ore/context\",\n" +
            namespaces +
            "  ],\n" +
            "\n" +
            "   \"describes\": {\n" +
            "     \"@type\": \"Aggregation\",\n" +
            "     \"isDescribedBy\":  { \"@embed\": false } ,\n" +
            "     \"aggregates\":  { \"@embed\": true }  ,\n" +
            "     \"proxies\":  { \"@embed\": true }\n" +
            "   }\n" +
            " }";

        context.set(JsonLD10Writer.JSONLD_FRAME, contextStr);

        var writer = RDFWriter.create()
            .format(RDFFormat.JSONLD10_FRAME_PRETTY)
            .source(DatasetFactory.wrap(model).asDatasetGraph())
            .context(context)
            .build();

        var outputWriter = new StringWriter();
        writer.output(outputWriter);

        return outputWriter.toString();
    }

    private String namespacesAsJsonObject(Map<String, String> namespaces) {
        try {
            return objectMapper.writeValueAsString(namespaces);
        }
        catch (JsonProcessingException e) {
            log.error("Error serializing namespaces to JSON", e);
            throw new RuntimeException(e);
        }
    }
}
