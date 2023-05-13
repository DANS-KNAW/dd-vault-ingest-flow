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

import nl.knaw.dans.vaultingest.core.domain.DepositBag;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonDepositTest {

    @Test
    void getTitle() throws Exception {
        var props = Mockito.mock(CommonDepositProperties.class);
        var bag = Mockito.mock(DepositBag.class);

        var ddm = new XmlReaderImpl().readXmlFile(
            Path.of(getClass().getResource("/xml/example-ddm.xml").getPath())
        );

        var deposit = new CommonDeposit(
            "id", ddm, null, props, bag
        );

        assertEquals("A bag containing examples for each mapping rule", deposit.getTitle());
    }

    @Test
    void getDescription() throws Exception {
        var props = Mockito.mock(CommonDepositProperties.class);
        var bag = Mockito.mock(DepositBag.class);

        var ddm = new XmlReaderImpl().readXmlFile(
            Path.of(getClass().getResource("/xml/example-ddm.xml").getPath())
        );

        var deposit = new CommonDeposit(
            "id", ddm, null, props, bag
        );

        assertThat(deposit.getDescriptions())
            .extracting("value")
            .containsOnly(
                "This bags contains one or more examples of each mapping rule.",
                "A second description",
                "some date",
                "some acceptance date",
                "some copyright date",
                "some submission date",
                "some modified date",
                "some issuing date",
                "some validation date",
                "some coverage description",
                "Even more descriptions"
            );
        System.out.println(deposit.getDescriptions());
    }

    @Test
    void getOtherIds() throws Exception {
        var props = Mockito.mock(CommonDepositProperties.class);
        var bag = Mockito.mock(DepositBag.class);

        Mockito.when(bag.getMetadataValue(Mockito.any()))
            .thenReturn(List.of("DANS:12345"));

        var ddm = new XmlReaderImpl().readXmlFile(
            Path.of(getClass().getResource("/xml/example-ddm.xml").getPath())
        );

        var deposit = new CommonDeposit(
            "id", ddm, null, props, bag
        );

        assertThat(deposit.getOtherIds())
            .extracting("fullName")
            .containsOnly(
                "DCTERMS_ID001",
                "DCTERMS_ID002",
                "DCTERMS_ID003",
                "DANS:12345"
            );
    }

    @Test
    void getAuthors() throws Exception {
        var props = Mockito.mock(CommonDepositProperties.class);
        var bag = Mockito.mock(DepositBag.class);

        var ddm = new XmlReaderImpl().readXmlFile(
            Path.of(getClass().getResource("/xml/example-ddm.xml").getPath())
        );

        var deposit = new CommonDeposit(
            "id", ddm, null, props, bag
        );

        assertThat(deposit.getAuthors())
            .extracting("displayName")
            .containsOnly(
                "Unformatted Creator",
                "I Lastname",
                "Creator Organization"
            );

        assertThat(deposit.getAuthors())
            .extracting("identifierScheme")
            .containsOnly(
                null,
                "ISNI",
                "VIAF"
            );

        assertThat(deposit.getAuthors())
            .extracting("identifier")
            .containsOnly(
                null,
                "6666 7777 8888 9999",
                "123456789"
            );
    }
}