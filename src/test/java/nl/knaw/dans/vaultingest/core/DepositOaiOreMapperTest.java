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
package nl.knaw.dans.vaultingest.core;

import nl.knaw.dans.vaultingest.core.domain.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.TestDeposit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DepositOaiOreMapperTest {

    @Test
    void mapDepositToOaiOre() throws Exception {
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

        System.out.println("DEPOSIT: " + deposit);
        var converter = new DepositOaiOreMapper();
        var output = converter.mapDepositToOaiOre(deposit);

        System.out.println("RDF: " + output.toRDF());
        System.out.println("JSON: " + output.toJsonLD());

//        var output2 = converter.mapDepositToOaiOre(deposit);
//
//        System.out.println("RDF: " + output2.toRDF());
//        System.out.println("JSON: " + output2.toJsonLD());
    }
}