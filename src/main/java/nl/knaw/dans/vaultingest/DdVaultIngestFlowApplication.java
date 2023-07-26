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
import nl.knaw.dans.vaultcatalog.client.ApiClient;
import nl.knaw.dans.vaultcatalog.client.OcflObjectVersionApi;
import nl.knaw.dans.vaultingest.client.BagValidator;
import nl.knaw.dans.vaultingest.client.MigrationBagValidator;
import nl.knaw.dans.vaultingest.client.VaultCatalogClient;
import nl.knaw.dans.vaultingest.core.DepositToBagProcess;
import nl.knaw.dans.vaultingest.core.util.IdMinter;
import nl.knaw.dans.vaultingest.core.deposit.CsvLanguageResolver;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.deposit.DepositOutbox;
import nl.knaw.dans.vaultingest.core.deposit.FileCountryResolver;
import nl.knaw.dans.vaultingest.core.deposit.MigrationDepositManager;
import nl.knaw.dans.vaultingest.core.inbox.AutoIngestArea;
import nl.knaw.dans.vaultingest.core.inbox.IngestAreaDirectoryWatcher;
import nl.knaw.dans.vaultingest.core.inbox.MigrationIngestArea;
import nl.knaw.dans.vaultingest.core.rdabag.DefaultRdaBagWriterFactory;
import nl.knaw.dans.vaultingest.core.rdabag.output.ZipBagOutputWriterFactory;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import nl.knaw.dans.vaultingest.health.DansBagValidatorHealthCheck;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.io.IOException;

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

        var languageResolver = new CsvLanguageResolver(
            configuration.getIngestFlow().getLanguages().getIso6391(),
            configuration.getIngestFlow().getLanguages().getIso6392()
        );

        var countryResolver = new FileCountryResolver(
            configuration.getIngestFlow().getSpatialCoverageCountryTermsPath()
        );
        var xmlReader = new XmlReader();
        var depositValidator = new BagValidator(dansBagValidatorClient, configuration.getValidateDansBag().getValidateUrl());
        var depositManager = new DepositManager(xmlReader);

        var rdaBagWriterFactory = new DefaultRdaBagWriterFactory(
            environment.getObjectMapper(),
            languageResolver,
            countryResolver
        );

        var outputWriterFactory = new ZipBagOutputWriterFactory(configuration.getIngestFlow().getRdaBagOutputDir());

        var ocflObjectVersionApi = createOcflObjectVersionApi(configuration, environment);
        var vaultCatalogRepository = new VaultCatalogClient(ocflObjectVersionApi);
        var idMinter = new IdMinter();

        var depositToBagProcess = new DepositToBagProcess(
            rdaBagWriterFactory,
            outputWriterFactory,
            vaultCatalogRepository,
            depositValidator,
            idMinter,
            depositManager
        );

        var taskQueue = configuration.getIngestFlow().getTaskQueue().build(environment);

        var ingestAreaDirectoryWatcher = new IngestAreaDirectoryWatcher(
            500,
            configuration.getIngestFlow().getAutoIngest().getInbox()
        );

        var autoIngestOutbox = new DepositOutbox(configuration.getIngestFlow().getAutoIngest().getOutbox());
        var inboxListener = new AutoIngestArea(
            taskQueue,
            ingestAreaDirectoryWatcher,
            depositToBagProcess,
            autoIngestOutbox
        );

        var migrationDepositValidator = new MigrationBagValidator(dansBagValidatorClient, configuration.getValidateDansBag().getValidateUrl());
        var migrationDepositManager = new MigrationDepositManager(xmlReader);

        var migrationDepositToBagProcess = new DepositToBagProcess(
            rdaBagWriterFactory,
            outputWriterFactory,
            vaultCatalogRepository,
            migrationDepositValidator,
            idMinter,
            migrationDepositManager
        );

        var migrationIngestArea = new MigrationIngestArea(
            taskQueue,
            migrationDepositToBagProcess,
            configuration.getIngestFlow().getMigration().getInbox(),
            new DepositOutbox(configuration.getIngestFlow().getMigration().getOutbox())
        );

        inboxListener.start();

        environment.healthChecks().register(
            "DansBagValidator",
            new DansBagValidatorHealthCheck(
                dansBagValidatorClient, configuration.getValidateDansBag().getPingUrl()
            )
        );
    }

    OcflObjectVersionApi createOcflObjectVersionApi(DdVaultIngestFlowConfiguration configuration, Environment environment) {
        var client = new JerseyClientBuilder(environment)
            .using(configuration.getVaultCatalog().getHttpClient())
            .build("vault-catalog");

        var apiClient = new ApiClient();
        apiClient.setHttpClient(client);
        apiClient.setBasePath(configuration.getVaultCatalog().getUrl().toString());

        return new OcflObjectVersionApi(apiClient);
    }
}
