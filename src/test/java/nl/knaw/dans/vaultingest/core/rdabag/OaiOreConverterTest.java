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
package nl.knaw.dans.vaultingest.core.rdabag;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.dans.vaultingest.core.domain.ids.DAI;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.metadata.Description;
import nl.knaw.dans.vaultingest.core.domain.metadata.OtherId;
import nl.knaw.dans.vaultingest.core.rdabag.converter.OaiOreConverter;
import nl.knaw.dans.vaultingest.core.rdabag.serializer.OaiOreSerializer;
import nl.knaw.dans.vaultingest.core.utilities.TestDeposit;
import nl.knaw.dans.vaultingest.core.utilities.TestDepositFile;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.vocabulary.DCTerms;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OaiOreConverterTest {
    private final String NBN = "urn:nbn:nl:ui:13-69bef523-0488-4268-bdef-18a9a347017b";

    @Test
        // TODO this was just for checking output, see if it can be reused
    void convert() {
        var deposit = TestDeposit.builder()
            .id(UUID.randomUUID().toString())
            .doi("doi:10.17026/dans-12345")
            .nbn(NBN)
            .title("The beautiful title")
            .descriptions(List.of(
                Description.builder().value("Description 1").build(),
                Description.builder().value("Description 2").build()
            ))
            .authors(List.of(
                DatasetAuthor.builder()
                    .initials("EJ")
                    .name("Eric")
                    .affiliation("Affiliation 1")
                    .dai(new DAI("123456"))
                    .build(),
                DatasetAuthor.builder()
                    .name("Somebody")
                    .build()
            ))
            .subject("Something about science")
            .rightsHolder(List.of("John Rights"))
            .alternativeTitles(List.of("alt title 1", "alt title 2"))
            .otherIds(List.of(
                OtherId.builder().value("without agency").build(),
                OtherId.builder().value("agency 1").agency("Agency name").build()
            ))
            .payloadFiles(List.of(
                TestDepositFile.builder()
                    .id(UUID.randomUUID().toString())
                    .path(Path.of("data/valid/characters.txt"))
                    .build()
            ))
            .build();

        var converter = new OaiOreConverter();
        var output = converter.convert(deposit);
        var serializer = new OaiOreSerializer(new ObjectMapper());

        System.out.println("RDF: " + serializer.serializeAsRdf(output));
        System.out.println("JSON: " + serializer.serializeAsJsonLd(output));
    }

    @Test
    void title() {
        var deposit = TestDeposit.builder()
            .nbn(NBN)
            .title("Hello there")
            .build();

        var converter = new OaiOreConverter();
        var output = converter.convert(deposit);

        var resource = output.getModel().getResource(NBN);

        var statements = output.getModel().listStatements(
            new SimpleSelector(resource, DCTerms.title, (RDFNode) null)
        ).toList();

        assertThat(statements)
            .extracting("object")
            .map(Object::toString)
            .containsOnly("Hello there");
    }
}