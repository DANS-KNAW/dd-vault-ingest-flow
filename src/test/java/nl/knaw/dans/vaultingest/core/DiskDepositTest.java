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

import nl.knaw.dans.vaultingest.core.deposit.CommonDepositManager;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.rdabag.RdaBagWriter;
import nl.knaw.dans.vaultingest.core.utilities.EchoDatasetContactResolver;
import nl.knaw.dans.vaultingest.core.utilities.StdoutBagOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.TestLanguageResolver;
import nl.knaw.dans.vaultingest.core.validator.BagValidator;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogService;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

class DiskDepositTest {

    @Test
    void process() throws Exception {
        var rdaBagWriter = new RdaBagWriter();
        var xmlReader = new XmlReaderImpl();
        var vaultCatalogService = Mockito.mock(VaultCatalogService.class);
        var depositValidator = Mockito.mock(BagValidator.class);
        var depositManager = Mockito.mock(DepositManager.class);
        var depositToBagProcess = new DepositToBagProcess(rdaBagWriter, (p) -> new StdoutBagOutputWriter(), vaultCatalogService, depositManager, depositValidator, new IdMinter());

        var s = getClass().getResource("/input/6a6632f1-91d2-49ba-8449-a8d2b539267a");
        assert s != null;
        var depositDir = Path.of(s.getPath());

        var deposit = new CommonDepositManager(xmlReader, new EchoDatasetContactResolver(), new TestLanguageResolver()).loadDeposit(depositDir);

        depositToBagProcess.processDeposit(deposit);
    }
}