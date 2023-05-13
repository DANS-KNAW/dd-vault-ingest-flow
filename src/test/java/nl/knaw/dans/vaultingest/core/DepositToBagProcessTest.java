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

import nl.knaw.dans.vaultingest.core.domain.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.DepositBag;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.Description;
import nl.knaw.dans.vaultingest.core.domain.TestDeposit;
import nl.knaw.dans.vaultingest.core.rdabag.DepositRdaBagConverter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

class DepositToBagProcessTest {

    @Test
    void process() throws IOException {
        var depositRdaBagConverter = new DepositRdaBagConverter();
        var rdaBagWriter = new RdaBagWriter();
        var depositToBagProcess = new DepositToBagProcess(depositRdaBagConverter, (deposit) -> {
        }, rdaBagWriter);

        var s = getClass().getResource("/input/6a6632f1-91d2-49ba-8449-a8d2b539267a/valid-bag");
        assert s != null;
        var bagDir = Path.of(s.getPath());
        Files.walk(bagDir)
            .filter(Files::isRegularFile)
            .forEach(System.out::println);

        var bag = new DepositBag() {

            private final List<DepositFile> files = List.of(
                new DepositFile(UUID.randomUUID().toString(), Path.of("data/file/1.txt")),
                new DepositFile(UUID.randomUUID().toString(), Path.of("data/file/2.txt")),
                new DepositFile(UUID.randomUUID().toString(), Path.of("data/subdir/a/b/c/file.txt")),
                new DepositFile(UUID.randomUUID().toString(), Path.of("data/subdir 2/document.xlsx"))
            );

            @Override
            public Collection<DepositFile> getPayloadFiles() {
                return files;
            }

            @Override
            public InputStream inputStreamForPayloadFile(DepositFile depositFile) {
                var str = "Contents of file " + depositFile.getId() + "\n";
                return new ByteArrayInputStream(str.getBytes());
            }

            @Override
            public Collection<Path> getMetadataFiles() {
                return List.of(
                    Path.of("metadata/dataset.xml"),
                    Path.of("metadata/files.xml"),
                    Path.of("metadata/agreements.xml")
                );
            }

            @Override
            public InputStream inputStreamForMetadataFile(Path path) {
                var str = "Contents of file " + path + "\n";
                return new ByteArrayInputStream(str.getBytes());
            }

            @Override
            public InputStream getBagInfoFile() {
                var str = "Payload-Oxum: 0.2\n" +
                    "Bagging-Date: 2018-05-25\n" +
                    "Bag-Size: 2.5 KB\n" +
                    "Created: 2018-11-16T00:00:00.000+02:00\n";

                return new ByteArrayInputStream(str.getBytes());
            }

            @Override
            public InputStream getBagItFile() {
                var str = "BagIt-Version: 0.97\n" +
                    "Tag-File-Character-Encoding: UTF-8\n";

                return new ByteArrayInputStream(str.getBytes());
            }

            @Override
            public List<String> getMetadataValue(String key) {
                return List.of();
            }

        };

        System.out.println("BAGDIR: " + bagDir);
        var deposit = TestDeposit.builder()
            .id("doi:10.17026/dans-12345")
            .title("The beautiful title")
            .bag(bag)
            .descriptions(List.of(
                Description.builder().value("Description 1").build(),
                Description.builder().value("Description 2").build()
            ))
            .authors(List.of(
                DatasetAuthor.builder()
                    .initials("EJ")
                    .name("Eric")
                    .affiliation("Affiliation 1")
                    .dai("123456")
                    .build()
            ))
            .subject("Something about science")
            .rightsHolder("John Rights")
            .build();

        depositToBagProcess.process(deposit);
    }
}