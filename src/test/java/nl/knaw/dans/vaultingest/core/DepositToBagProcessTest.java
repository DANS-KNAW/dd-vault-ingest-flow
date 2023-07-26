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
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.deposit.Outbox;
import nl.knaw.dans.vaultingest.core.rdabag.DefaultRdaBagWriterFactory;
import nl.knaw.dans.vaultingest.core.rdabag.RdaBagWriter;
import nl.knaw.dans.vaultingest.core.util.IdMinter;
import nl.knaw.dans.vaultingest.core.utilities.CountryResolverFactory;
import nl.knaw.dans.vaultingest.core.utilities.InMemoryOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.LanguageResolverFactory;
import nl.knaw.dans.vaultingest.core.utilities.NullBagOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.TestDepositManager;
import nl.knaw.dans.vaultingest.core.validator.BagValidator;
import nl.knaw.dans.vaultingest.core.validator.InvalidDepositException;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogDeposit;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepositToBagProcessTest {

    @Test
    void process_should_handle_deposit_correctly() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositValidator = Mockito.mock(BagValidator.class);

        Mockito.when(vaultCatalogService.registerDeposit(Mockito.any()))
            .thenReturn(VaultCatalogDeposit.builder().objectVersion(1L).build());

        var deposit = getBasicDeposit();
        var depositManager = TestDepositManager.ofDeposit(deposit);

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

        assertThat(output.isClosed()).isTrue();
        assertThat(depositManager.isSaveDepositCalled()).isTrue();

        assertThat(deposit.getState()).isEqualTo(Deposit.State.ACCEPTED);
        assertThat(deposit.getStateDescription()).isEqualTo("Deposit accepted");
    }

    @Test
    void process_should_move_deposit_to_failed_outbox_when_DepositManager_throws_exception() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositValidator = Mockito.mock(BagValidator.class);

        Mockito.when(vaultCatalogService.registerDeposit(Mockito.any()))
            .thenReturn(VaultCatalogDeposit.builder().objectVersion(1L).build());

        var depositManager = Mockito.spy(TestDepositManager.ofDeposit(null));
        Mockito.doThrow(new RuntimeException("error")).when(depositManager).loadDeposit(Mockito.any());

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

        assertThat(depositManager.getLastState()).isEqualTo(Deposit.State.FAILED);
        assertThat(depositManager.getLastMessage()).isNotEmpty();
    }

    @Test
    void process_should_move_deposit_to_REJECTED_outbox_when_validator_throws_exception() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositValidator = Mockito.mock(BagValidator.class);

        Mockito.when(vaultCatalogService.registerDeposit(Mockito.any()))
            .thenReturn(VaultCatalogDeposit.builder().objectVersion(1L).build());

        Mockito.doThrow(new InvalidDepositException("Invalid deposit!"))
            .when(depositValidator).validate(Mockito.any());

        var deposit = getBasicDeposit();
        var depositManager = TestDepositManager.ofDeposit(deposit);
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

        assertThat(depositManager.getLastState()).isEqualTo(Deposit.State.REJECTED);
        assertThat(depositManager.getLastMessage()).isEqualTo("Invalid deposit!");
    }

    @Test
    void process_should_move_deposit_to_REJECTED_outbox_when_vaultCatalog_returns_no_result_for_update() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositValidator = Mockito.mock(BagValidator.class);

        Mockito.when(vaultCatalogService.registerDeposit(Mockito.any()))
            .thenReturn(VaultCatalogDeposit.builder().objectVersion(1L).build());

        Mockito.when(vaultCatalogService.findDeposit(Mockito.any()))
            .thenReturn(Optional.empty());

        var deposit = getBasicDepositAsUpdate();
        var depositManager = TestDepositManager.ofDeposit(deposit);
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

        assertThat(depositManager.getLastState()).isEqualTo(Deposit.State.REJECTED);
    }

    @Test
    void processDeposit() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(BagValidator.class);
        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            deposit -> output,
            vaultCatalogService,
            depositValidator,
            new IdMinter(),
            depositManager);

        Mockito.when(vaultCatalogService.registerDeposit(Mockito.any()))
            .thenReturn(VaultCatalogDeposit.builder().objectVersion(1L).build());

        var deposit = getBasicDeposit();
        depositToBagProcess.processDeposit(deposit);

        assertThat(output.isClosed()).isTrue();
        assertThat(output.getData().keySet())
            .map(Path::toString)
            .containsOnly(
                "bag-info.txt",
                "bagit.txt",
                "manifest-sha1.txt",
                "tagmanifest-sha1.txt",
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
        var depositValidator = Mockito.mock(BagValidator.class);

        Mockito.when(vaultCatalogService.registerDeposit(Mockito.any()))
            .thenReturn(VaultCatalogDeposit.builder().objectVersion(1L).build());

        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            d -> new NullBagOutputWriter(),
            vaultCatalogService,
            depositValidator, new IdMinter(), depositManager
        );

        depositToBagProcess.processDeposit(deposit);

        assertThat(deposit.getState()).isEqualTo(Deposit.State.ACCEPTED);
    }

    @Test
    void process_should_fail_if_update_cannot_be_found() throws Exception {
        var deposit = getBasicDepositAsUpdate();
        var rdaBagWriter = getWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);

        Mockito.when(vaultCatalogService.registerDeposit(Mockito.any()))
            .thenReturn(VaultCatalogDeposit.builder().objectVersion(1L).build());

        Mockito.doReturn(Optional.empty())
            .when(vaultCatalogService).findDeposit(Mockito.any());

        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(BagValidator.class);

        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            d -> new NullBagOutputWriter(),
            vaultCatalogService,
            depositValidator, new IdMinter(), depositManager);

        assertThatThrownBy(() -> depositToBagProcess.processDeposit(deposit))
            .isInstanceOf(InvalidDepositException.class);
    }

    @Test
    void process_should_write_sha1_if_only_md5_is_present() throws Exception {
        var rdaBagWriter = getWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogRepository.class);
        var depositValidator = Mockito.mock(BagValidator.class);

        Mockito.when(vaultCatalogService.registerDeposit(Mockito.any()))
            .thenReturn(VaultCatalogDeposit.builder().objectVersion(1L).build());

        var deposit = Mockito.spy(getBasicDeposit());
        var depositManager = TestDepositManager.ofDeposit(deposit);

        Mockito.doReturn(Set.of(StandardSupportedAlgorithms.MD5))
            .when(deposit).getPayloadManifestAlgorithms();

        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            value -> output,
            vaultCatalogService,
            depositValidator,
            new IdMinter(),
            depositManager);

        var outbox = Mockito.mock(Outbox.class);

        depositToBagProcess.process(Path.of("input/path/"), outbox);

        assertThat(output.getData().entrySet())
            .extracting(Map.Entry::getKey)
            .contains(
                Path.of("manifest-md5.txt"),
                Path.of("manifest-sha1.txt"),
                Path.of("tagmanifest-md5.txt"),
                Path.of("tagmanifest-sha1.txt")
            );
    }

    private Deposit getBasicDeposit() {
        var manager = new TestDepositManager();
        return manager.loadDeposit(Path.of("/input/integration-test-complete-bag/c169676f-5315-4d86-bde0-a62dbc915228/"));
    }

    private Deposit getBasicDepositAsUpdate() {
        var manager = new TestDepositManager();
        var deposit = manager.loadDeposit(Path.of("/input/integration-test-complete-bag/c169676f-5315-4d86-bde0-a62dbc915228/"));

        var spied = Mockito.spy(deposit);

        Mockito.doReturn(true).when(spied).isUpdate();
        Mockito.doReturn("sword:abc").when(spied).getSwordToken();

        return spied;
    }

    private RdaBagWriter getWriter() throws Exception {
        return new DefaultRdaBagWriterFactory(
            new ObjectMapper(),
            LanguageResolverFactory.getInstance(),
            CountryResolverFactory.getInstance()
        ).createRdaBagWriter();
    }
}