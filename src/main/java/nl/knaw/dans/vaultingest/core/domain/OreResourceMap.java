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
package nl.knaw.dans.vaultingest.core.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
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

@Data
@AllArgsConstructor
public class OreResourceMap {
    private final Model model;
    private final Map<String, String> namespaces;

    public String toRDF() {
        // the order of this is important, it dictates the output
        var topLevelResources = new Resource[]{
                model.createResource("http://www.openarchives.org/ore/terms/AggregatedResource"),
                model.createResource("http://www.openarchives.org/ore/terms/Aggregation"),
                model.createResource("http://www.openarchives.org/ore/terms/ResourceMap")
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

    public String toJsonLD() throws JsonProcessingException {
        var context = new Context();
        var namespaces = namespacesAsJsonObject(this.namespaces);
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

    private String namespacesAsJsonObject(Map<String, String> namespaces) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(namespaces);
    }

}
