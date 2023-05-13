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

import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.reader.BagReader;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiskDepositLoader {
    private final XmlReader xmlReader = new XmlReaderImpl();

    public Deposit loadDeposit(Path path) {
        try {
            // get bag dir
            var bagDir = Files.list(path)
                .filter(Files::isDirectory)
                .findFirst()
                .orElseThrow();

            var ddm = bagDir.resolve(Path.of("metadata", "dataset.xml"));
            var filesXml = bagDir.resolve(Path.of("metadata", "files.xml"));

            var propertiesFile = path.resolve("deposit.properties");
            var params = new Parameters();
            var paramConfig = params.properties()
                .setFileName(propertiesFile.toString());

            var builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>
                (PropertiesConfiguration.class, null, true)
                .configure(paramConfig);

            var properties = new CommonDepositProperties(builder.getConfiguration());

            var bag = new BagReader().read(bagDir);
            var depositBag = new CommonDepositBag(bag);

            return CommonDeposit.builder()
                .id(path.getFileName().toString())
                .ddm(xmlReader.readXmlFile(ddm))
                .filesXml(xmlReader.readXmlFile(filesXml))
                .properties(properties)
                .depositBag(depositBag)
                .build();
        }
        catch (IOException | SAXException | ParserConfigurationException | ConfigurationException | MaliciousPathException | UnparsableVersionException | UnsupportedAlgorithmException |
               InvalidBagitFileFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
