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
package nl.knaw.dans.vaultingest.core.deposit;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.reader.BagReader;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CommonDepositBagIntegrationTest {

    @Test
    void getMetadataFiles() throws Exception {
        var bag = getBag();
        var depositBag = new CommonDepositBag(bag);

        var metadataFiles = depositBag.getMetadataFiles();

        assertThat(metadataFiles).containsOnly(
            Path.of("metadata/files.xml"),
            Path.of("metadata/dataset.xml")
        );
    }

    @Test
    void inputStreamForMetadataFile() throws Exception {
        var bag = getBag();
        var depositBag = new CommonDepositBag(bag);

        try (var file = depositBag.inputStreamForMetadataFile(Path.of("metadata/dataset.xml"))) {
            var data = new String(file.readAllBytes());

            // check if it read the complete xml file
            assertDoesNotThrow(() -> {
                new XmlReaderImpl().readXmlString(data);
            });

            assertTrue(data.startsWith("<ddm:DDM"));
        }
    }

    @Test
    void getMetadataValue() throws Exception {
        var bag = getBag();
        var depositBag = new CommonDepositBag(bag);

        assertEquals("3212481.4", depositBag.getMetadataValue("Payload-Oxum").get(0));
        assertEquals("2022-10-23", depositBag.getMetadataValue("Bagging-Date").get(0));
        assertEquals("3.1 MB", depositBag.getMetadataValue("Bag-Size").get(0));
        assertEquals("2016-11-12T23:41:11.000+00:00", depositBag.getMetadataValue("Created").get(0));
        assertEquals("REPO1:1234", depositBag.getMetadataValue("Has-Organizational-Identifier").get(0));
        assertEquals("1.1", depositBag.getMetadataValue("Has-Organizational-Identifier-Version").get(0));

        assertEquals(0, depositBag.getMetadataValue("Does-Not-Exist").size());
    }

    Bag getBag() throws Exception {
        var bagPath = "/input/integration-test-complete-bag/c169676f-5315-4d86-bde0-a62dbc915228/audiences/";
        var resource = Path.of(Objects.requireNonNull(getClass().getResource(bagPath)).getPath());

        return new BagReader().read(resource);
    }
}