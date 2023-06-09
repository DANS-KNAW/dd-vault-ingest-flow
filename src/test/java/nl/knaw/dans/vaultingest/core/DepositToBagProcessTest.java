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
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.ids.DAI;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.metadata.Description;
import nl.knaw.dans.vaultingest.core.rdabag.DefaultRdaBagWriterFactory;
import nl.knaw.dans.vaultingest.core.rdabag.RdaBagWriter;
import nl.knaw.dans.vaultingest.core.utilities.InMemoryOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.NullBagOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.TestDeposit;
import nl.knaw.dans.vaultingest.core.utilities.TestDepositFile;
import nl.knaw.dans.vaultingest.core.validator.DepositValidator;
import nl.knaw.dans.vaultingest.core.validator.InvalidDepositException;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DepositToBagProcessTest {

    @Test
    void process() throws Exception {
        var rdaBagWriter = new DefaultRdaBagWriterFactory(new ObjectMapper()).createRdaBagWriter();
        var output = new InMemoryOutputWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogService.class);
        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(DepositValidator.class);
        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            deposit -> output,
            vaultCatalogService,
            depositManager,
            depositValidator,
            new IdMinter()
        );

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
                "data/file1.txt",
                "data/file2.txt",
                "metadata/dataset.xml",
                "metadata/files.xml",
                "metadata/oai-ore.rdf",
                "metadata/oai-ore.jsonld",
                "metadata/pid-mapping.txt",
                "metadata/datacite.xml"
            );
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
        var depositValidator = Mockito.mock(DepositValidator.class);

        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            d -> new NullBagOutputWriter(),
            vaultCatalogService,
            depositManager, depositValidator, new IdMinter());

        depositToBagProcess.processDeposit(deposit);

        assertEquals(Deposit.State.ACCEPTED, deposit.getState());
    }

    @Test
    void process_should_fail_if_update_cannot_be_found() {
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

        var rdaBagWriter = new DefaultRdaBagWriterFactory(new ObjectMapper()).createRdaBagWriter();
        var vaultCatalogService = Mockito.mock(VaultCatalogService.class);

        Mockito.doReturn(Optional.empty())
            .when(vaultCatalogService).findDeposit(Mockito.any());

        var depositManager = Mockito.mock(DepositManager.class);
        var depositValidator = Mockito.mock(DepositValidator.class);

        var depositToBagProcess = new DepositToBagProcess(
            () -> rdaBagWriter,
            d -> new NullBagOutputWriter(),
            vaultCatalogService,
            depositManager, depositValidator, new IdMinter());

        assertThrows(InvalidDepositException.class, () -> depositToBagProcess.processDeposit(deposit));
    }
}