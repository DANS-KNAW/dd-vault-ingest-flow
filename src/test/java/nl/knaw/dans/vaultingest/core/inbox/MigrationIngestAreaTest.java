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
package nl.knaw.dans.vaultingest.core.inbox;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import nl.knaw.dans.vaultingest.core.DepositToBagProcess;
import nl.knaw.dans.vaultingest.core.utilities.TestOutbox;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MigrationIngestAreaTest {

    @Test
    void getAbsolutePath_should_work_with_relativePath() {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(DepositToBagProcess.class);
        var outbox = new TestOutbox(Path.of("/outbox/path/"));
        var area = new MigrationIngestArea(executor, process, Path.of("/input/path/"), outbox);

        var result = area.getAbsolutePath(Path.of("batch1"));
        assertEquals(Path.of("/input/path/batch1"), result);
    }

    @Test
    void getAbsolutePath_should_work_with_absolutePath() {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(DepositToBagProcess.class);
        var outbox = new TestOutbox(Path.of("/outbox/path/"));
        var area = new MigrationIngestArea(executor, process, Path.of("/input/path/"), outbox);

        var result = area.getAbsolutePath(Path.of("/this/is/absolute"));
        assertEquals(Path.of("/this/is/absolute"), result);
    }

    @Test
    void validateDepositDir_should_work() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(DepositToBagProcess.class);
        var outbox = new TestOutbox(Path.of("/outbox/path/"));
        var area = new MigrationIngestArea(executor, process, Path.of("/input/path/"), outbox);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var path = fs.getPath("/input/path/batch1/deposit1");
            Files.createDirectories(path);
            Files.write(path.resolve("deposit.properties"), "state=FAILED".getBytes());

            assertDoesNotThrow(() -> area.validateDepositDirectory(path));
        }
    }

    @Test
    void validateBatchDirectory_should_handle_multiple_deposits() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(DepositToBagProcess.class);
        var outbox = new TestOutbox(Path.of("/outbox/path/"));
        var area = new MigrationIngestArea(executor, process, Path.of("/input/path/"), outbox);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var path = fs.getPath("/input/path/batch1/");
            Files.createDirectories(path.resolve("deposit1"));
            Files.createDirectories(path.resolve("deposit2"));
            Files.write(path.resolve("deposit1/deposit.properties"), "".getBytes());
            Files.write(path.resolve("deposit2/deposit.properties"), "".getBytes());

            assertDoesNotThrow(() -> area.validateBatchDirectory(path));
        }
    }

    @Test
    void ingest_should_handle_paths_without_issues() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(DepositToBagProcess.class);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new TestOutbox(fs.getPath("/outbox/path/"));
            var area = new MigrationIngestArea(executor, process, fs.getPath("/input/path/"), outbox);

            var path = fs.getPath("/input/path/batch1/");
            Files.createDirectories(path.resolve("deposit1"));
            Files.createDirectories(path.resolve("deposit2"));
            Files.write(path.resolve("deposit1/deposit.properties"), "".getBytes());
            Files.write(path.resolve("deposit2/deposit.properties"), "".getBytes());

            area.ingest(path, true, false);

            Mockito.verify(executor, Mockito.times(2)).execute(Mockito.any());
        }
    }
}