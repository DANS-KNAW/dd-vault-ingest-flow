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

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.deposit.Outbox;
import nl.knaw.dans.vaultingest.core.rdabag.DefaultRdaBagWriterFactory;
import nl.knaw.dans.vaultingest.core.rdabag.RdaBagWriter;
import nl.knaw.dans.vaultingest.core.utilities.InMemoryOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.NullBagOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.TestCountryResolver;
import nl.knaw.dans.vaultingest.core.utilities.TestDepositManager;
import nl.knaw.dans.vaultingest.core.utilities.TestLanguageResolver;
import nl.knaw.dans.vaultingest.core.utilities.TestSimpleDepositManager;
import nl.knaw.dans.vaultingest.core.validator.DepositValidator;
import nl.knaw.dans.vaultingest.core.validator.InvalidDepositException;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositToBagProcessTest {

    @Test
    void process_should_handle_deposit_correctly() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositValidator = Mockito.mock(DepositValidator.class);

        var deposit = getBasicDeposit();
        var depositManager = new TestDepositManager(deposit);

        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            value -> output,
            vaultCatalogService,
            depositValidator,
            new IdMinter(),
            depositManager);

        var outbox = Mockito.mock(Outbox.class);

        depositToBagProcess.process(Path.of("input/path/"), outbox);

        Mockito.verify(outbox).moveDeposit(Mockito.any());

        assertTrue(output.isClosed());
        assertTrue(depositManager.isSaveDepositCalled());

        assertEquals(Deposit.State.ACCEPTED, deposit.getState());
        assertEquals("Deposit accepted", deposit.getStateDescription());
    }

    @Test
    void process_should_move_deposit_to_failed_outbox_when_DepositManager_throws_exception() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositValidator = Mockito.mock(DepositValidator.class);

        var depositManager = new TestDepositManager(null);
        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            value -> output,
            vaultCatalogService,
            depositValidator,
            new IdMinter(),
            depositManager);

        var outbox = Mockito.mock(Outbox.class);

        depositToBagProcess.process(Path.of("input/path/"), outbox);
        Mockito.verify(outbox).move(Path.of("input/path/"), Deposit.State.FAILED);

        assertEquals(Deposit.State.FAILED, depositManager.getLastState());
        assertTrue(depositManager.getLastMessage().length() > 0);
    }

    @Test
    void process_should_move_deposit_to_REJECTED_outbox_when_validator_throws_exception() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositValidator = Mockito.mock(DepositValidator.class);

        Mockito.doThrow(new InvalidDepositException("Invalid deposit!"))
            .when(depositValidator).validate(Mockito.any());

        var deposit = getBasicDeposit();
        var depositManager = new TestDepositManager(deposit);
        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            value -> output,
            vaultCatalogService,
            depositValidator,
            new IdMinter(),
            depositManager);

        var outbox = Mockito.mock(Outbox.class);

        depositToBagProcess.process(Path.of("input/path/"), outbox);
        Mockito.verify(outbox).move(Path.of("input/path/"), Deposit.State.REJECTED);

        assertEquals(Deposit.State.REJECTED, depositManager.getLastState());
        assertEquals("Invalid deposit!", depositManager.getLastMessage());
    }

    @Test
    void process_should_move_deposit_to_REJECTED_outbox_when_vaultCatalog_returns_no_result_for_update() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositValidator = Mockito.mock(DepositValidator.class);

        Mockito.when(vaultCatalogService.findDeposit(Mockito.any()))
            .thenReturn(Optional.empty());

        var deposit = getBasicDepositAsUpdate();
        var depositManager = new TestDepositManager(deposit);
        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            value -> output,
            vaultCatalogService,
            depositValidator,
            new IdMinter(),
            depositManager);

        var outbox = Mockito.mock(Outbox.class);

        depositToBagProcess.process(Path.of("input/path/"), outbox);
        Mockito.verify(outbox).move(Path.of("input/path/"), Deposit.State.REJECTED);

        assertEquals(Deposit.State.REJECTED, depositManager.getLastState());
    }

    @Test
    void processDeposit() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(DepositValidator.class);
        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            deposit -> output,
            vaultCatalogService,
            depositValidator,
            new IdMinter(),
            depositManager);

        var deposit = getBasicDeposit();
        depositToBagProcess.processDeposit(deposit);

        assertTrue(output.isClosed());
        assertThat(output.getData().keySet())
            .map(Path::toString)
            .containsOnly(
                "bag-info.txt",
                "bagit.txt",
                "manifest-sha1.txt",
                "manifest-md5.txt",
                "tagmanifest-sha1.txt",
                "tagmanifest-md5.txt",
                "metadata/dataset.xml",
                "metadata/files.xml",
                "metadata/oai-ore.rdf",
                "metadata/oai-ore.jsonld",
                "metadata/pid-mapping.txt",
                "metadata/datacite.xml",
                "data/random images/image02.jpeg",
                "data/random images/image03.jpeg",
                "data/random images/image01.png",
                "data/a/deeper/path/With some file.txt"
            );
    }

    @Test
    void process_should_process_nonUpdate_deposit() throws Exception {
        var deposit = getBasicDeposit();

        var rdaBagWriter = Mockito.mock(RdaBagWriter.class);
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(DepositValidator.class);

        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            d -> new NullBagOutputWriter(),
            vaultCatalogService,
            depositValidator, new IdMinter(), depositManager);

        depositToBagProcess.processDeposit(deposit);

        assertEquals(Deposit.State.ACCEPTED, deposit.getState());
    }

    @Test
    void process_should_fail_if_update_cannot_be_found() throws Exception {
        var deposit = getBasicDepositAsUpdate();
        var rdaBagWriter = getWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);

        Mockito.doReturn(Optional.empty())
            .when(vaultCatalogService).findDeposit(Mockito.any());

        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(DepositValidator.class);

        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            d -> new NullBagOutputWriter(),
            vaultCatalogService,
            depositValidator, new IdMinter(), depositManager);

        assertThrows(InvalidDepositException.class, () -> depositToBagProcess.processDeposit(deposit));
    }

    private Deposit getBasicDeposit() {
        var manager = new TestSimpleDepositManager();

        return manager.loadDeposit(
            Path.of("/input/integration-test-complete-bag/c169676f-5315-4d86-bde0-a62dbc915228/")
        );
    }

    private Deposit getBasicDepositAsUpdate() {
        var manager = new TestSimpleDepositManager();
        var deposit = manager.loadDeposit(
            Path.of("/input/integration-test-complete-bag/c169676f-5315-4d86-bde0-a62dbc915228/")
        );

        var spied = Mockito.spy(deposit);

        Mockito.doReturn(true).when(spied).isUpdate();
        Mockito.doReturn("sword:abc").when(spied).getSwordToken();

        return spied;
    }

    private RdaBagWriter getWriter() throws Exception {
        return new DefaultRdaBagWriterFactory(
            new ObjectMapper(),
            new TestLanguageResolver(),
            new TestCountryResolver()
        ).createRdaBagWriter();
    }
}