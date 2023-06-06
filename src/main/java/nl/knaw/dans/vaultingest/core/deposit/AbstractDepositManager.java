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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.domain.ManifestAlgorithm;
import nl.knaw.dans.vaultingest.core.domain.OriginalFilepaths;
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
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractDepositManager implements DepositManager {
    private final XmlReader xmlReader;

    protected Path getBagDir(Path path) throws IOException {
        try (var list = Files.list(path)) {
            return list.filter(Files::isDirectory)
                .findFirst()
                .orElseThrow();
        }
    }

    protected Document readXmlFile(Path path) throws IOException, SAXException, ParserConfigurationException {
        return xmlReader.readXmlFile(path);
    }

    protected CommonDepositProperties getDepositProperties(Path path) throws ConfigurationException {
        var propertiesFile = path.resolve("deposit.properties");
        var params = new Parameters();
        var paramConfig = params.properties()
            .setFileName(propertiesFile.toString());

        var builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>
            (PropertiesConfiguration.class, null, true)
            .configure(paramConfig);

        return new CommonDepositProperties(builder);
    }

    public void saveDepositProperties(CommonDepositProperties properties) throws ConfigurationException {
        var builder = properties.getBuilder();
        builder.save();
    }

    protected OriginalFilepaths getOriginalFilepaths(Path bagDir) throws IOException {
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

    protected Map<Path, Map<ManifestAlgorithm, String>> getPrecomputedChecksums(Path bagDir, Bag bag) {
        var manifests = new HashMap<Path, Map<ManifestAlgorithm, String>>();

        for (var manifest: bag.getPayLoadManifests()) {
            try {
                var alg = ManifestAlgorithm.from(manifest.getAlgorithm().getMessageDigestName());

                for (var entry: manifest.getFileToChecksumMap().entrySet()) {
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

    public void saveDeposit(Path path) {

    }
}
