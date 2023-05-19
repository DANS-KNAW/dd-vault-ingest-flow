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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DansRel;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.Datacite;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.ORE;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.SchemaDO;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class OreResourceMap {
    private final Model model;

    public Model getModel() {
        // TODO check this isnt too slow to do on every get
        // and that it is stable
        var namespaces = this.getUsedNamespaces();

        for (var namespace : namespaces.entrySet()) {
            model.setNsPrefix(namespace.getKey(), namespace.getValue());
        }

        return model;
    }

    /*
    TODO: figure out if filtering out unused namespaces is a hard requirement,
    or just for aesthetics.
     */
    public Map<String, String> getUsedNamespaces() {
        var predicateNamespaces = this.model.listStatements()
            .toList().stream()
            .map(Statement::getPredicate)
            .map(Property::getNameSpace)
            .collect(Collectors.toSet());

        log.trace("predicateNamespaces: {}", predicateNamespaces);

        return this.getNamespaceMap().entrySet()
            .stream().filter(entry -> predicateNamespaces.contains(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, String> getNamespaceMap() {
        var namespaces = new HashMap<String, String>();

        namespaces.put("cit", DVCitation.NS);
        namespaces.put("dcterms", DCTerms.NS);
        namespaces.put("datacite", Datacite.NS);
        namespaces.put("ore", ORE.NS);
        namespaces.put("dc", DC_11.NS);
        namespaces.put("foaf", FOAF.NS);
        namespaces.put("schema", SchemaDO.NS);
        namespaces.put("dansREL", DansRel.NS);

        return namespaces;
    }
}
