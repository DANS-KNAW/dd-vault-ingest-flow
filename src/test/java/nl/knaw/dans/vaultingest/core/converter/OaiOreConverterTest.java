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
package nl.knaw.dans.vaultingest.core.converter;

import nl.knaw.dans.vaultingest.core.DepositOaiOreMapper;
import nl.knaw.dans.vaultingest.core.domain.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.TestDeposit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OaiOreConverterTest {

    @Test
    void convert() throws Exception {
        var deposit = TestDeposit.builder()
                .id("doi:10.17026/dans-12345")
                .title("The beautiful title")
                .descriptions(List.of("Description 1", "Description 2"))
                .authors(List.of(
                        DatasetAuthor.builder()
                                .initials("EJ")
                                .name("Eric")
                                .affiliation("Affiliation 1")
                                .dai("123456")
                                .build()
                ))
                .subject("Something about science")
                .rightsHolder("John Rights")
                .build();

        var converter = new OaiOreConverter();
        var output = converter.convert(deposit);

        System.out.println("RDF: " + output.toRDF());
        System.out.println("JSON: " + output.toJsonLD());
    }
}