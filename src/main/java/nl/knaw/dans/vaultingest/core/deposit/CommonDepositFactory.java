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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.OriginalFilepaths;
import nl.knaw.dans.vaultingest.core.validator.InvalidDepositException;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class CommonDepositFactory {
    private final XmlReader xmlReader;
    private final DatasetContactResolver datasetContactResolver;
    private final LanguageResolver languageResolver;

    private final CommonDepositValidator commonDepositValidator;

    public CommonDepositFactory(XmlReader xmlReader, DatasetContactResolver datasetContactResolver, LanguageResolver languageResolver, CommonDepositValidator commonDepositValidator) {
        this.xmlReader = xmlReader;
        this.datasetContactResolver = datasetContactResolver;
        this.languageResolver = languageResolver;
        this.commonDepositValidator = commonDepositValidator;
    }

    public Deposit loadDeposit(Path path) throws InvalidDepositException {
        try {
            var bagDir = getBagDir(path);

            var ddm = readXmlFile(bagDir.resolve(Path.of("metadata", "dataset.xml")));
            var filesXml = readXmlFile(bagDir.resolve(Path.of("metadata", "files.xml")));

            var originalFilePaths = getOriginalFilepaths(bagDir);

            var depositProperties = getDepositProperties(path);
            var depositFiles = getDepositFiles(bagDir, ddm, filesXml, originalFilePaths);

            // TODO think about the validate step being in the loadDeposit
            // it makes sense because why would you want to load a bag that is invalid,
            // but it also breaks the SRP
            commonDepositValidator.validate(bagDir);

            return CommonDeposit.builder()
                .id(path.getFileName().toString())
                .ddm(ddm)
                .bag(new CommonDepositBag(bagDir))
                .filesXml(filesXml)
                .depositFiles(depositFiles)
                .properties(depositProperties)
                .datasetContactResolver(datasetContactResolver)
                .languageResolver(languageResolver)
                .build();

        }
        catch (IOException | SAXException | ParserConfigurationException | ConfigurationException e) {
            log.error("Error loading deposit from disk: path={}", path, e);
            throw new RuntimeException(e);
        }
    }

    Path getBagDir(Path path) throws IOException {
        try (var list = Files.list(path)) {
            return list.filter(Files::isDirectory)
                .findFirst()
                .orElseThrow();
        }
    }

    Document readXmlFile(Path path) throws IOException, SAXException, ParserConfigurationException {
        return xmlReader.readXmlFile(path);
    }

    CommonDepositProperties getDepositProperties(Path path) throws ConfigurationException {
        var propertiesFile = path.resolve("deposit.properties");
        var params = new Parameters();
        var paramConfig = params.properties()
            .setFileName(propertiesFile.toString());

        var builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>
            (PropertiesConfiguration.class, null, true)
            .configure(paramConfig);

        return new CommonDepositProperties(builder.getConfiguration());
    }

    OriginalFilepaths getOriginalFilepaths(Path bagDir) throws IOException {
        var originalFilepathsFile = bagDir.resolve("original-filepaths.txt");
        var result = new OriginalFilepaths();

        if (Files.exists(originalFilepathsFile)) {
            try (var lines = Files.lines(originalFilepathsFile)) {
                lines.filter(StringUtils::isNotBlank)
                    .map(line -> line.split("\\s+", 2))
                    .forEach(line -> result.addMapping(
                        Path.of(line[1]), Path.of(line[0]))
                    );
            }
        }

        return result;
    }

    List<DepositFile> getDepositFiles(Path bagDir, Document ddm, Document filesXml, OriginalFilepaths originalFilepaths) {

        return XPathEvaluator.nodes(filesXml, "/files:files/files:file")
            .map(node -> {
                var filePath = node.getAttributes().getNamedItem("filepath").getTextContent();
                var physicalPath = bagDir.resolve(originalFilepaths.getPhysicalPath(Path.of(filePath)));

                return CommonDepositFile.builder()
                    .id(UUID.randomUUID().toString())
                    .physicalPath(physicalPath)
                    .filesXmlNode(node)
                    .ddmNode(ddm)
                    .build();
            })
            .collect(Collectors.toList());
    }
}
