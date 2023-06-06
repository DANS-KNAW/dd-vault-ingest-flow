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
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.OriginalFilepaths;
import nl.knaw.dans.vaultingest.core.validator.InvalidBagException;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class CommonDepositManager extends AbstractDepositManager {
    private final DatasetContactResolver datasetContactResolver;
    private final LanguageResolver languageResolver;

    public CommonDepositManager(XmlReader xmlReader, DatasetContactResolver datasetContactResolver, LanguageResolver languageResolver) {
        super(xmlReader);
        this.datasetContactResolver = datasetContactResolver;
        this.languageResolver = languageResolver;
    }

    @Override
    public Deposit loadDeposit(Path path) throws InvalidBagException {
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

            return CommonDeposit.builder()
                .id(path.getFileName().toString())
                .path(path)
                .ddm(ddm)
                .bag(new CommonDepositBag(bag))
                .filesXml(filesXml)
                .depositFiles(depositFiles)
                .properties(depositProperties)
                .datasetContactResolver(datasetContactResolver)
                .languageResolver(languageResolver)
                .build();

        }
        catch (Exception e) {
            log.error("Error loading deposit from disk: path={}", path, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveDeposit(Deposit deposit) {
        if (!(deposit instanceof CommonDeposit)) {
            throw new IllegalArgumentException("Deposit is not a CommonDeposit");
        }

        var commonDeposit = (CommonDeposit) deposit;
        var properties = commonDeposit.getProperties();

        try {
            properties.getBuilder().save();
        }
        catch (ConfigurationException e) {
            log.error("Error saving deposit properties: depositId={}", deposit.getId(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateDepositState(Path path, Deposit.State state, String message) {
        try {
            var depositProperties = getDepositProperties(path);
            depositProperties.setProperty("state.label", state.name());
            depositProperties.setProperty("state.message", message);

            saveDepositProperties(depositProperties);
        }
        catch (ConfigurationException e) {
            log.error("Error updating deposit state: path={}, state={}, message={}", path, state, message, e);
            throw new RuntimeException(e);
        }

    }

    List<DepositFile> getDepositFiles(Path bagDir, Bag bag, Document ddm, Document filesXml, OriginalFilepaths originalFilepaths) {
        var manifests = getPrecomputedChecksums(bagDir, bag);

        return XPathEvaluator.nodes(filesXml, "/files:files/files:file")
            .map(node -> {
                var filePath = node.getAttributes().getNamedItem("filepath").getTextContent();
                var physicalPath = bagDir.resolve(originalFilepaths.getPhysicalPath(Path.of(filePath)));
                var checksums = manifests.get(bagDir.relativize(physicalPath));

                return CommonDepositFile.builder()
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
