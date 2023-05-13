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
package nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class DVCitation {
    public static final String NS = "https://dataverse.org/schema/citation/";
    private static final Model m = ModelFactory.createDefaultModel();
    public static final Resource NAMESPACE = m.createResource(NS);
    public static final Property author = m.createProperty(NS, "author");
    public static final Property authorName = m.createProperty(NS, "authorName");
    public static final Property authorAffiliation = m.createProperty(NS, "authorAffiliation");
    public static final Property authorIdentifierScheme = m.createProperty(NS, "authorIdentifierScheme");
    public static final Property authorIdentifier = m.createProperty(NS, "authorIdentifier");

    public static String getURI() {
        return NS;
    }
}
