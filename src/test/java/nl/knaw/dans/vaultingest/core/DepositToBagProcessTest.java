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

import nl.knaw.dans.vaultingest.core.deposit.DiskDepositLoader;
import nl.knaw.dans.vaultingest.core.domain.*;
import nl.knaw.dans.vaultingest.core.domain.ids.DAI;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.metadata.Description;
import nl.knaw.dans.vaultingest.core.rdabag.RdaBagWriter;
import nl.knaw.dans.vaultingest.core.rdabag.output.StdoutBagOutputWriter;
import nl.knaw.dans.vaultingest.core.rdabag.output.ZipBagOutputWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class DepositToBagProcessTest {

    @Test
    void process() throws IOException {
        var rdaBagWriter = new RdaBagWriter();
        var depositToBagProcess = new DepositToBagProcess((deposit) -> {
        }, rdaBagWriter, new StdoutBagOutputWriter());

        var s = getClass().getResource("/input/6a6632f1-91d2-49ba-8449-a8d2b539267a/valid-bag");
        assert s != null;
        var bagDir = Path.of(s.getPath());
        Files.walk(bagDir)
            .filter(Files::isRegularFile)
            .forEach(System.out::println);

        var deposit = TestDeposit.builder()
            .id("doi:10.17026/dans-12345")
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
            .rightsHolder("John Rights")
            .build();

        depositToBagProcess.process(deposit);
    }

    @Test
    void process_with_originalFilePathMappings() throws IOException {
        var rdaBagWriter = new RdaBagWriter();
        var depositToBagProcess = new DepositToBagProcess((deposit) -> {
        }, rdaBagWriter, new StdoutBagOutputWriter());

        var s = getClass().getResource("/input/0b9bb5ee-3187-4387-bb39-2c09536c79f7");
        assert s != null;

        var bagDir = Path.of(s.getPath());

        var deposit = new DiskDepositLoader().loadDeposit(bagDir);

        depositToBagProcess.process(deposit);
    }

    @Test
    void process_with_originalFilePathMappings_to_zip() throws IOException {
        var rdaBagWriter = new RdaBagWriter();
        var output = new ZipBagOutputWriter(Path.of("/tmp/bag123.zip"));
        var depositToBagProcess = new DepositToBagProcess((deposit) -> {
        }, rdaBagWriter, output);

        var s = getClass().getResource("/input/0b9bb5ee-3187-4387-bb39-2c09536c79f7");
        assert s != null;

        var bagDir = Path.of(s.getPath());

        var deposit = new DiskDepositLoader().loadDeposit(bagDir);

        depositToBagProcess.process(deposit);
    }
}