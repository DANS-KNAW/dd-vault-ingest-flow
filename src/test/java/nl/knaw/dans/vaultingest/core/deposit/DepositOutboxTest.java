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
package nl.knaw.dans.vaultingest.core.deposit;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepositOutboxTest {

    @Test
    void moveDeposit_should_move_deposit_to_outbox() throws Exception {
        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new DepositOutbox(fs.getPath("/outbox/path/"));
            outbox.init(true);

            Files.createDirectories(fs.getPath("/input/path/deposit1"));
            Files.createDirectories(fs.getPath("/input/path/deposit2"));
            Files.createDirectories(fs.getPath("/input/path/deposit3"));

            outbox.move(fs.getPath("/input/path/deposit1"), Deposit.State.FAILED);
            outbox.move(fs.getPath("/input/path/deposit2"), Deposit.State.REJECTED);
            outbox.move(fs.getPath("/input/path/deposit3"), Deposit.State.ACCEPTED);

            assertThat(Files.exists(fs.getPath("/outbox/path/failed/deposit1")))
                .isTrue();
            assertThat(Files.exists(fs.getPath("/outbox/path/rejected/deposit2")))
                .isTrue();
            assertThat(Files.exists(fs.getPath("/outbox/path/processed/deposit3")))
                .isTrue();
        }
    }

    @Test
    void moveDeposit_should_not_accept_unknown_states() throws Exception {
        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new DepositOutbox(fs.getPath("/outbox/path/"));
            outbox.init(true);

            Files.createDirectories(fs.getPath("/input/path/deposit1"));

            assertThatThrownBy(() -> outbox.move(fs.getPath("/input/path/deposit1"), Deposit.State.DRAFT))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> outbox.move(fs.getPath("/input/path/deposit1"), Deposit.State.INVALID))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> outbox.move(fs.getPath("/input/path/deposit1"), Deposit.State.UPLOADED))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void moveDeposit_should_throw_IllegalStateException_when_init_is_not_called() throws Exception {
        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new DepositOutbox(fs.getPath("/outbox/path/"));
            Files.createDirectories(fs.getPath("/input/path/deposit1"));

            assertThatThrownBy(() -> outbox.move(fs.getPath("/input/path/deposit1"), Deposit.State.ACCEPTED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("init()");
        }
    }

    @Test
    void moveDeposit_should_not_throw_error_when_allowNonEmpty_is_false_and_target_is_empty() throws Exception {
        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new DepositOutbox(fs.getPath("/outbox/path/"));
            outbox.init(false);
        }
    }

    @Test
    void moveDeposit_should_throw_IllegalStateException_when_target_has_content() throws Exception {
        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new DepositOutbox(fs.getPath("/outbox/path/"));
            Files.createDirectories(fs.getPath("/outbox/path/failed/deposit1"));

            assertThatThrownBy(() -> outbox.init(false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not empty");
        }
    }

    @Test
    void moveDeposit_should_not_throw_IllegalStateException_when_output_root_directories_exist() throws Exception {
        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new DepositOutbox(fs.getPath("/outbox/path/"));
            Files.createDirectories(fs.getPath("/outbox/path/failed"));

            assertThatNoException().isThrownBy(() -> outbox.init(false));
        }
    }

    @Test
    void withBatchDirectory_should_reference_subdirectory() throws Exception {
        try (var fs = MemoryFileSystemBuilder.newLinux().build()) {
            var outbox = new DepositOutbox(fs.getPath("/outbox/path/"));
            var outbox2 = outbox.withBatchDirectory(fs.getPath("batch1"));
            outbox2.init(false);

            assertThat(Files.exists(fs.getPath("/outbox/path/batch1/failed"))).isTrue();
            assertThat(Files.exists(fs.getPath("/outbox/path/batch1/processed"))).isTrue();
            assertThat(Files.exists(fs.getPath("/outbox/path/batch1/rejected"))).isTrue();
        }
    }
}