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
import nl.knaw.dans.vaultingest.core.ConvertToRdaBagTask;
import nl.knaw.dans.vaultingest.core.utilities.TestOutbox;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrationIngestAreaTest {

    @Test
    void getAbsolutePath_should_work_with_relativePath() {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(ConvertToRdaBagTask.class);
        var outbox = new TestOutbox(Path.of("/outbox/path/"));
        var area = new MigrationIngestArea(executor, process, Path.of("/input/path/"), outbox, Map.of());

        var result = area.getAbsolutePath(Path.of("batch1"));
        assertThat(result).isEqualTo(Path.of("/input/path/batch1"));
    }

    @Test
    void getAbsolutePath_should_work_with_absolutePath() {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(ConvertToRdaBagTask.class);
        var outbox = new TestOutbox(Path.of("/outbox/path/"));
        var area = new MigrationIngestArea(executor, process, Path.of("/input/path/"), outbox, Map.of());

        var result = area.getAbsolutePath(Path.of("/this/is/absolute"));
        assertThat(result).isEqualTo(Path.of("/this/is/absolute"));
    }

    @Test
    void validateDepositDir_should_work() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(ConvertToRdaBagTask.class);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new TestOutbox(fs.getPath("/outbox/path/"));
            var area = new MigrationIngestArea(executor, process, fs.getPath("/input/path/"), outbox, Map.of());
            var path = fs.getPath("/input/path/batch1/deposit1");
            Files.createDirectories(path);
            Files.write(path.resolve("deposit.properties"), "state=FAILED".getBytes());

            assertThatNoException().isThrownBy(() -> area.validateDepositDirectory(path));
        }
    }

    @Test
    void validateDepositDir_should_throw_IllegalArgumentException_if_depositProperties_is_absent() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(ConvertToRdaBagTask.class);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new TestOutbox(fs.getPath("/outbox/path/"));
            var area = new MigrationIngestArea(executor, process, fs.getPath("/input/path/"), outbox, Map.of());
            var path = fs.getPath("/input/path/batch1/deposit1");
            Files.createDirectories(path);

            assertThatThrownBy(() -> area.validateDepositDirectory(path))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not contain file deposit.properties");
        }
    }

    @Test
    void validateBatchDirectory_should_handle_multiple_deposits() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(ConvertToRdaBagTask.class);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new TestOutbox(fs.getPath("/outbox/path/"));
            var area = new MigrationIngestArea(executor, process, fs.getPath("/input/path/"), outbox, Map.of());

            var path = fs.getPath("/input/path/batch1/");
            Files.createDirectories(path.resolve("deposit1"));
            Files.createDirectories(path.resolve("deposit2"));
            Files.write(path.resolve("deposit1/deposit.properties"), "".getBytes());
            Files.write(path.resolve("deposit2/deposit.properties"), "".getBytes());

            assertThatNoException().isThrownBy(() -> area.validateBatchDirectory(path));
        }
    }

    @Test
    void validateBatchDirectory_should_throw_IllegalArgumentException_if_one_deposit_is_invalid() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(ConvertToRdaBagTask.class);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new TestOutbox(fs.getPath("/outbox/path/"));
            var area = new MigrationIngestArea(executor, process, fs.getPath("/input/path/"), outbox, Map.of());

            var path = fs.getPath("/input/path/batch1/");
            Files.createDirectories(path.resolve("deposit1"));
            Files.createDirectories(path.resolve("deposit2"));
            Files.write(path.resolve("deposit1/deposit.properties"), "".getBytes());
            // no deposit.properties for deposit2

            assertThatThrownBy(() -> area.validateBatchDirectory(path))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deposit2 does not contain file deposit.properties");
        }
    }

    @Test
    void validateBatchDirectory_should_throw_IllegalArgumentException_if_inbox_is_not_a_directory() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(ConvertToRdaBagTask.class);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new TestOutbox(fs.getPath("/outbox/path/"));
            var area = new MigrationIngestArea(executor, process, fs.getPath("/input/path/"), outbox, Map.of());

            var path = fs.getPath("/input/path/batch1");
            Files.createDirectories(path.getParent());
            Files.write(path, "file".getBytes());

            assertThatThrownBy(() -> area.validateBatchDirectory(path))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not a directory");
        }
    }

    @Test
    void ingest_should_handle_paths_without_issues() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(ConvertToRdaBagTask.class);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new TestOutbox(fs.getPath("/outbox/path/"));
            var area = new MigrationIngestArea(executor, process, fs.getPath("/input/path/"), outbox, Map.of());

            var path = fs.getPath("/input/path/batch1/");
            Files.createDirectories(path.resolve("deposit1"));
            Files.createDirectories(path.resolve("deposit2"));
            Files.write(path.resolve("deposit1/deposit.properties"), "".getBytes());
            Files.write(path.resolve("deposit2/deposit.properties"), "".getBytes());

            area.ingest(path, true, false);

            Mockito.verify(executor, Mockito.times(2)).execute(Mockito.any());
        }
    }

    @Test
    void ingest_should_reject_paths_outside_inboxDir() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(ConvertToRdaBagTask.class);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new TestOutbox(fs.getPath("/outbox/path/"));
            var area = new MigrationIngestArea(executor, process, fs.getPath("/input/path/"), outbox, Map.of());

            var path = fs.getPath("/random/other/deposit1");
            Files.createDirectories(path);
            Files.write(path.resolve("deposit.properties"), "".getBytes());

            assertThatThrownBy(() -> area.ingest(path, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be subdirectory of /input/path");

        }
    }

    @Test
    void ingest_should_handle_single_deposit() throws Exception {
        var executor = Mockito.mock(ExecutorService.class);
        var process = Mockito.mock(ConvertToRdaBagTask.class);

        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new TestOutbox(fs.getPath("/outbox/path/"));
            var area = new MigrationIngestArea(executor, process, fs.getPath("/input/path/"), outbox, Map.of());

            var path = fs.getPath("/input/path/deposit1");
            Files.createDirectories(path);
            Files.write(path.resolve("deposit.properties"), "".getBytes());

            assertThatNoException().isThrownBy(() -> area.ingest(path, false, false));
        }
    }
}