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

import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCore;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DansRel;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.Datacite;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.ORE;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.SchemaDO;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// TODO should not be in domain
public class OreNamespaces {
    private final static Map<String, String> namespaces = new HashMap<>();

    static {
        namespaces.put("cit", DVCitation.NS);
        namespaces.put("dcterms", DCTerms.NS);
        namespaces.put("datacite", Datacite.NS);
        namespaces.put("ore", ORE.NS);
        namespaces.put("dc", DC_11.NS);
        namespaces.put("foaf", FOAF.NS);
        namespaces.put("schema", SchemaDO.NS);
        namespaces.put("dansREL", DansRel.NS);
        namespaces.put("dvcore", DVCore.NS);
    }

    public static Map<String, String> getNamespaces() {
        return Collections.unmodifiableMap(namespaces);
    }
}
