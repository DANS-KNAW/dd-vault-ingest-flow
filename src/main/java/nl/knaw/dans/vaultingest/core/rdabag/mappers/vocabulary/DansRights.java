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

public class DansRights {
    public static final String NS = "https://dar.dans.knaw.nl/schema/2023.04/dansRights#";
    private static final Model m = ModelFactory.createDefaultModel();
    public static final Property dansRightsHolder = m.createProperty(NS, "dansRightsHolder");
    public static final Property dansPersonalDataPresent = m.createProperty(NS, "dansPersonalDataPresent");
    public static final Property dansMetadataLanguage = m.createProperty(NS, "dansMetadataLanguage");
}
