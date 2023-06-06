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
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.TestDeposit;
import nl.knaw.dans.vaultingest.core.domain.TestDepositFile;
import nl.knaw.dans.vaultingest.core.domain.ids.DAI;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.metadata.Description;
import nl.knaw.dans.vaultingest.core.rdabag.RdaBagWriter;
import nl.knaw.dans.vaultingest.core.rdabag.output.ZipBagOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.EchoDatasetContactResolver;
import nl.knaw.dans.vaultingest.core.utilities.NullBagOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.StdoutBagOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.TestLanguageResolver;
import nl.knaw.dans.vaultingest.core.validator.BagValidator;
import nl.knaw.dans.vaultingest.core.validator.InvalidBagException;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogService;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DepositToBagProcessTest {

    @Test
    void process() throws Exception {
        var rdaBagWriter = new RdaBagWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogService.class);
        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(BagValidator.class);
        var depositToBagProcess = new DepositToBagProcess(rdaBagWriter,
            deposit -> new StdoutBagOutputWriter(),
            vaultCatalogService, depositManager, depositValidator, new IdMinter());

        var s = getClass().getResource("/input/6a6632f1-91d2-49ba-8449-a8d2b539267a/valid-bag");
        assert s != null;
        var bagDir = Path.of(s.getPath());

        try (var files = Files.walk(bagDir)) {
            files.filter(Files::isRegularFile).forEach(System.out::println);
        }

        var deposit = TestDeposit.builder()
            .id(UUID.randomUUID().toString())
            .doi("doi:10.17026/dans-12345")
            .title("The beautiful title")
            .descriptions(List.of(
                Description.builder().value("Description 1").build(),
                Description.builder().value("Description 2").build()
            ))
            .authors(List.of(
                DatasetAuthor.builder()
                    .initials("EJ")
                    .name("Eric")
                    .affiliation("Affiliation 1")
                    .dai(new DAI("123456"))
                    .build()
            ))
            .subject("Something about science")
            .rightsHolder(List.of("John Rights"))
            .payloadFiles(List.of(
                TestDepositFile.builder()
                    .path(Path.of("data/file1.txt"))
                    .checksums(Map.of())
                    .id(UUID.randomUUID().toString())
                    .build(),
                TestDepositFile.builder()
                    .path(Path.of("data/file2.txt"))
                    .checksums(Map.of())
                    .id(UUID.randomUUID().toString())
                    .build()
            ))
            .build();

        depositToBagProcess.processDeposit(deposit);

        System.out.println(deposit);
    }

    @Test
    void process_with_originalFilePathMappings() throws Exception {
        var rdaBagWriter = new RdaBagWriter();
        var xmlReader = new XmlReaderImpl();
        var vaultCatalogService = Mockito.mock(VaultCatalogService.class);
        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(BagValidator.class);
        var depositToBagProcess = new DepositToBagProcess(rdaBagWriter,
            deposit -> new StdoutBagOutputWriter(),
            vaultCatalogService, depositManager, depositValidator, new IdMinter());

        var s = getClass().getResource("/input/0b9bb5ee-3187-4387-bb39-2c09536c79f7");
        assert s != null;

        var bagDir = Path.of(s.getPath());

        var deposit = new CommonDepositManager(xmlReader, new EchoDatasetContactResolver(), new TestLanguageResolver()).loadDeposit(bagDir);

        depositToBagProcess.processDeposit(deposit);
    }

    @Test
    void process_with_originalFilePathMappings_to_zip() throws Exception {
        var xmlReader = new XmlReaderImpl();
        var rdaBagWriter = new RdaBagWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogService.class);
        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(BagValidator.class);

        var output = new ZipBagOutputWriter(Path.of("/tmp/bag123.zip"));
        var depositToBagProcess = new DepositToBagProcess(rdaBagWriter, (path) -> output, vaultCatalogService, depositManager, depositValidator, new IdMinter());
        var s = getClass().getResource("/input/0b9bb5ee-3187-4387-bb39-2c09536c79f7");
        assert s != null;

        var bagDir = Path.of(s.getPath());
        var deposit = new CommonDepositManager(xmlReader, new EchoDatasetContactResolver(), new TestLanguageResolver()).loadDeposit(bagDir);

        depositToBagProcess.processDeposit(deposit);
    }

    @Test
    void process_should_process_nonUpdate_deposit() throws Exception {
        var deposit = TestDeposit.builder()
            .id(UUID.randomUUID().toString())
            .payloadFiles(List.of(
                TestDepositFile.builder()
                    .path(Path.of("data/file1.txt"))
                    .checksums(Map.of())
                    .id(UUID.randomUUID().toString())
                    .build()
            ))
            .build();

        var rdaBagWriter = Mockito.mock(RdaBagWriter.class);
        var vaultCatalogService = Mockito.mock(VaultCatalogService.class);
        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(BagValidator.class);

        var depositToBagProcess = new DepositToBagProcess(
            rdaBagWriter,
            d -> new NullBagOutputWriter(),
            vaultCatalogService,
            depositManager, depositValidator, new IdMinter());

        depositToBagProcess.processDeposit(deposit);

        assertEquals(Deposit.State.ACCEPTED, deposit.getState());
    }

    @Test
    void process_should_fail_if_update_cannot_be_found() throws Exception {
        var deposit = TestDeposit.builder()
            .id(UUID.randomUUID().toString())
            .update(true)
            .swordToken("sword-token")
            .payloadFiles(List.of(
                TestDepositFile.builder()
                    .path(Path.of("data/file1.txt"))
                    .checksums(Map.of())
                    .id(UUID.randomUUID().toString())
                    .build()
            ))
            .build();

        var rdaBagWriter = Mockito.mock(RdaBagWriter.class);
        var vaultCatalogService = Mockito.mock(VaultCatalogService.class);

        Mockito.doReturn(Optional.empty())
            .when(vaultCatalogService).findDeposit(Mockito.any());

        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(BagValidator.class);

        var depositToBagProcess = new DepositToBagProcess(
            rdaBagWriter,
            d -> new NullBagOutputWriter(),
            vaultCatalogService,
            depositManager, depositValidator, new IdMinter());

        assertThrows(InvalidBagException.class, () -> depositToBagProcess.processDeposit(deposit));
    }
}