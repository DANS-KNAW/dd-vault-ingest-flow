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

package nl.knaw.dans.vaultingest;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.config.IngestFlowConfig;
import nl.knaw.dans.vaultingest.core.DepositToBagProcess;
import nl.knaw.dans.vaultingest.core.deposit.CommonDepositFactory;
import nl.knaw.dans.vaultingest.core.deposit.CommonDepositValidator;
import nl.knaw.dans.vaultingest.core.deposit.HashMapLanguageResolver;
import nl.knaw.dans.vaultingest.core.deposit.LanguageResolver;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetContact;
import nl.knaw.dans.vaultingest.core.inbox.AutoIngestArea;
import nl.knaw.dans.vaultingest.core.inbox.IngestAreaDirectoryWatcher;
import nl.knaw.dans.vaultingest.core.inbox.ProcessDepositTaskFactory;
import nl.knaw.dans.vaultingest.core.rdabag.RdaBagWriter;
import nl.knaw.dans.vaultingest.core.rdabag.output.ZipBagOutputWriterFactory;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DdVaultIngestFlowApplication extends Application<DdVaultIngestFlowConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DdVaultIngestFlowApplication().run(args);
    }

    @Override
    public String getName() {
        return "Dd Vault Ingest Flow";
    }

    @Override
    public void initialize(final Bootstrap<DdVaultIngestFlowConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final DdVaultIngestFlowConfiguration configuration, final Environment environment) throws IOException {

        var dansBagValidatorClient = new JerseyClientBuilder(environment)
            .withProvider(MultiPartFeature.class)
            .using(configuration.getValidateDansBag().getHttpClient())
            .build(getName());

        var languageResolver = getLanguageResolver(configuration.getIngestFlow());
        var xmlReader = new XmlReaderImpl();
        var depositValidator = new CommonDepositValidator(dansBagValidatorClient, configuration.getValidateDansBag().getBaseUrl());
        var depositFactory = new CommonDepositFactory(
            xmlReader,
            userId -> DatasetContact.builder().name(userId).email(userId + "@test.com").build(),
            languageResolver,
            depositValidator
        );

        var rdaBagWriter = new RdaBagWriter();
        var outputWriterFactory = new ZipBagOutputWriterFactory(configuration.getIngestFlow().getRdaBagOutputDir());

        var depositToBagProcess = new DepositToBagProcess(
            rdaBagWriter,
            outputWriterFactory,
            deposit -> System.out.println("Deposit: " + deposit.getId())
        );

        var taskQueue = configuration.getIngestFlow().getTaskQueue().build(environment);

        var processDepositTaskFactory = new ProcessDepositTaskFactory(
            depositFactory,
            depositToBagProcess
        );

        var ingestAreaDirectoryWatcher = new IngestAreaDirectoryWatcher(
            500,
            configuration.getIngestFlow().getAutoIngest().getInbox()
        );

        var inboxListener = new AutoIngestArea(
            taskQueue,
            processDepositTaskFactory,
            ingestAreaDirectoryWatcher,
            configuration.getIngestFlow().getAutoIngest().getOutbox()
        );

        inboxListener.start();
    }

    private LanguageResolver getLanguageResolver(IngestFlowConfig config) {
        var iso1 = readLanguageCsv(config.getLanguages().getIso6391(), "ISO639-1");
        var iso2 = readLanguageCsv(config.getLanguages().getIso6392(), "ISO639-2");

        return new HashMapLanguageResolver(iso1, iso2);
    }

    private Map<String, String> readLanguageCsv(Path path, String keyColumn) {
        try {
            try (var parser = CSVParser.parse(path, StandardCharsets.UTF_8, CSVFormat.RFC4180.withFirstRecordAsHeader())) {
                var result = new HashMap<String, String>();

                for (var record : parser) {
                    result.put(record.get(keyColumn), record.get("Dataverse-language"));
                }

                return result;
            }
        }
        catch (Exception e) {
            log.error("Could not load csv", e);
            return Map.of();
        }
    }
}
