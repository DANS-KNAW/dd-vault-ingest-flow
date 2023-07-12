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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class DepositManager {
    private final XmlReader xmlReader;

    public Deposit loadDeposit(Path path) {
        try {
            var bagDir = getBagDir(path);

            log.info("Reading bag from path {}", bagDir);
            var bag = new BagReader().read(bagDir);

            log.info("Reading metadata/dataset.xml from path {}", bagDir);
            var ddm = readXmlFile(bagDir.resolve(Path.of("metadata", "dataset.xml")));

            log.info("Reading metadata/files.xml from path {}", bagDir);
            var filesXml = readXmlFile(bagDir.resolve(Path.of("metadata", "files.xml")));

            log.info("Generating original file paths if file exists");
            var originalFilePaths = getOriginalFilepaths(bagDir);

            log.info("Reading deposit.properties on path {}", path);
            var depositProperties = getDepositProperties(path);

            log.info("Generating payload file list on path {}", path);
            var depositFiles = getDepositFiles(bagDir, bag, ddm, filesXml, originalFilePaths);

            var builder = Deposit.builder()
                .id(path.getFileName().toString())
                .path(path)
                .ddm(ddm)
                .bag(new DepositBag(bag))
                .filesXml(filesXml)
                .depositFiles(depositFiles)
                .properties(depositProperties);

            builder = customizeDeposit(builder, depositProperties);

            return builder.build();

        }
        catch (Exception e) {
            log.error("Error loading deposit from disk: path={}", path, e);
            throw new RuntimeException(e);
        }
    }

    Deposit.DepositBuilder customizeDeposit(Deposit.DepositBuilder builder, DepositProperties depositProperties) {
        return builder;
    }

    public void saveDeposit(Deposit deposit) {
        var properties = deposit.getProperties();

        try {
            properties.save();
        }
        catch (ConfigurationException e) {
            log.error("Error saving deposit properties: depositId={}", deposit.getId(), e);
            throw new RuntimeException(e);
        }
    }

    public void updateDepositState(Path path, Deposit.State state, String message) {
        try {
            var depositProperties = getDepositProperties(path);
            depositProperties.setStateLabel(state.name());
            depositProperties.setStateDescription(message);

            depositProperties.save();
        }
        catch (ConfigurationException e) {
            log.error("Error updating deposit state: path={}, state={}, message={}", path, state, message, e);
            throw new RuntimeException(e);
        }
    }

    private Path getBagDir(Path path) throws IOException {
        try (var list = Files.list(path)) {
            return list.filter(Files::isDirectory)
                .findFirst()
                .orElseThrow();
        }
    }

    private Document readXmlFile(Path path) throws IOException, SAXException, ParserConfigurationException {
        return xmlReader.readXmlFile(path);
    }

    private DepositProperties getDepositProperties(Path path) throws ConfigurationException {
        var propertiesFile = path.resolve("deposit.properties");
        var params = new Parameters();
        var paramConfig = params.properties()
            .setFileName(propertiesFile.toString());

        var builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>
            (PropertiesConfiguration.class, null, true)
            .configure(paramConfig);

        return new DepositProperties(builder);
    }

    private OriginalFilepaths getOriginalFilepaths(Path bagDir) throws IOException {
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

    private Map<Path, Map<ManifestAlgorithm, String>> getPrecomputedChecksums(Path bagDir, Bag bag) {
        var manifests = new HashMap<Path, Map<ManifestAlgorithm, String>>();

        for (var manifest : bag.getPayLoadManifests()) {
            try {
                var alg = ManifestAlgorithm.from(manifest.getAlgorithm().getMessageDigestName());

                for (var entry : manifest.getFileToChecksumMap().entrySet()) {
                    var relativePath = bagDir.relativize(entry.getKey());
                    var checksum = entry.getValue();

                    manifests.computeIfAbsent(relativePath, k -> new HashMap<>())
                        .put(alg, checksum);
                }
            }
            catch (NoSuchAlgorithmException e) {
                log.warn("Bag contains a checksum algorithm that is not supported: algorithm={}",
                    manifest.getAlgorithm().getMessageDigestName(), e);
            }
        }

        return manifests;
    }

    private List<DepositFile> getDepositFiles(Path bagDir, Bag bag, Document ddm, Document filesXml, OriginalFilepaths originalFilepaths) {
        var manifests = getPrecomputedChecksums(bagDir, bag);

        return XPathEvaluator.nodes(filesXml, "/files:files/files:file")
            .map(node -> {
                var filePath = node.getAttributes().getNamedItem("filepath").getTextContent();
                var physicalPath = bagDir.resolve(originalFilepaths.getPhysicalPath(Path.of(filePath)));
                var checksums = manifests.get(bagDir.relativize(physicalPath));

                return DepositFile.builder()
                    .id(UUID.randomUUID().toString())
                    .physicalPath(physicalPath)
                    .filesXmlNode(node)
                    .ddmNode(ddm)
                    .checksums(checksums)
                    .build();
            })
            .collect(Collectors.toList());
    }

}
