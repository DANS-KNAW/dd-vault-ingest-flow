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
import nl.knaw.dans.vaultingest.core.deposit.CommonDepositFactory;
import nl.knaw.dans.vaultingest.core.rdabag.converter.OaiOreConverter;
import nl.knaw.dans.vaultingest.core.rdabag.serializer.OaiOreSerializer;
import nl.knaw.dans.vaultingest.core.utilities.EchoDatasetContactResolver;
import nl.knaw.dans.vaultingest.core.utilities.TestLanguageResolver;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Objects;

class OaiOreConverterIntegrationTest {

    @Test
    void convert() throws Exception {
        var s = getClass().getResource("/input/6a6632f1-91d2-49ba-8449-a8d2b539267a/");
        assert s != null;

        var xmlReader = Mockito.spy(new XmlReaderImpl());

        var testXmlPath = Path.of(Objects.requireNonNull(getClass().getResource("/xml/example-ddm.xml")).getPath());
        var testXml = xmlReader.readXmlFile(testXmlPath);

        var depositDir = Path.of(s.toURI());
        var toMockPath = depositDir.resolve("valid-bag/metadata/dataset.xml");

        // insert a different dataset.xml into the deposit
        Mockito.doReturn(testXml).when(xmlReader).readXmlFile(toMockPath);

        var deposit = new CommonDepositFactory(xmlReader, new EchoDatasetContactResolver(), new TestLanguageResolver()).loadDeposit(depositDir);
        var converter = new OaiOreConverter();
        var output = converter.convert(deposit);
        var serializer = new OaiOreSerializer(new ObjectMapper());

        var model = output.getModel();
        System.out.println("RDF: " + serializer.serializeAsRdf(output));
        System.out.println("JSON: " + serializer.serializeAsJsonLd(output));
    }
}