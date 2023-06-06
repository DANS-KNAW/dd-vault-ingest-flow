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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;

import java.util.Map;
import java.util.stream.Collectors;

// TODO should not be in domain (or should it?)
@AllArgsConstructor
@Slf4j
public class OreResourceMap {
    private final Model model;

    public Model getModel() {
        // TODO check this isnt too slow to do on every get
        // and that it is stable
        var namespaces = this.getUsedNamespaces();

        for (var namespace: namespaces.entrySet()) {
            model.setNsPrefix(namespace.getKey(), namespace.getValue());
        }

        return model;
    }

    public Map<String, String> getUsedNamespaces() {
        var predicateNamespaces = this.model.listStatements()
            .toList().stream()
            .map(Statement::getPredicate)
            .map(Property::getNameSpace)
            .collect(Collectors.toSet());

        log.trace("predicateNamespaces: {}", predicateNamespaces);

        return OreNamespaces.getNamespaces().entrySet()
            .stream().filter(entry -> predicateNamespaces.contains(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
