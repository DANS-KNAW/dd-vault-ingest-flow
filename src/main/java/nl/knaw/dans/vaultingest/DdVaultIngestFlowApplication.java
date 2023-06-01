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
import nl.knaw.dans.vaultingest.core.DepositToBagProcess;
import nl.knaw.dans.vaultingest.core.deposit.CommonDepositFactory;
import nl.knaw.dans.vaultingest.core.deposit.CommonDepositValidator;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetContact;
import nl.knaw.dans.vaultingest.core.inbox.AutoIngestArea;
import nl.knaw.dans.vaultingest.core.rdabag.RdaBagWriter;
import nl.knaw.dans.vaultingest.core.rdabag.output.ZipBagOutputWriterFactory;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.io.IOException;

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

        final var dansBagValidatorClient = new JerseyClientBuilder(environment)
            .withProvider(MultiPartFeature.class)
            .using(configuration.getValidateDansBag().getHttpClient())
            .build(getName());

        var xmlReader = new XmlReaderImpl();
        var depositValidator = new CommonDepositValidator(dansBagValidatorClient, configuration.getValidateDansBag().getBaseUrl());
        var depositFactory = new CommonDepositFactory(
            xmlReader,
            userId -> DatasetContact.builder().name(userId).email(userId + "@test.com").build(),
            language -> language,
            depositValidator
        );

        var rdaBagWriter = new RdaBagWriter();
        var outputWriterFactory = new ZipBagOutputWriterFactory(configuration.getIngestFlow().getRdaBagOutputDir());

        var depositToBagProcess = new DepositToBagProcess(
            rdaBagWriter,
            outputWriterFactory,
            deposit -> {

            }
        );

        var taskQueue = configuration.getIngestFlow().getTaskQueue().build(environment);

        var inboxListener = new AutoIngestArea(
            taskQueue,
            depositFactory,
            depositToBagProcess,
            configuration.getIngestFlow().getAutoIngest().getInbox(),
            configuration.getIngestFlow().getAutoIngest().getOutbox()
        );
    }

}
