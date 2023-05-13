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
package nl.knaw.dans.vaultingest.core.rdabag.mappers;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.SchemaDO;

import java.util.List;

public class Title {

    // CIT001
    public static List<Statement> toTitle(Resource resource, String title) {
        var model = resource.getModel();
        var literal = model.createLiteral(title);

        return List.of(
            model.createStatement(resource, DCTerms.title, literal),
            model.createStatement(resource, SchemaDO.name, literal)
        );
    }
}
