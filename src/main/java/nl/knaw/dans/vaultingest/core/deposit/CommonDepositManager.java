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

import gov.loc.repository.bagit.reader.BagReader;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.validator.InvalidDepositException;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.nio.file.Path;

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
    public Deposit loadDeposit(Path path) throws InvalidDepositException {
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
            properties.save();
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
            depositProperties.setStateLabel(state.name());
            depositProperties.setStateDescription(message);

            depositProperties.save();
        }
        catch (ConfigurationException e) {
            log.error("Error updating deposit state: path={}, state={}, message={}", path, state, message, e);
            throw new RuntimeException(e);
        }
    }

}
