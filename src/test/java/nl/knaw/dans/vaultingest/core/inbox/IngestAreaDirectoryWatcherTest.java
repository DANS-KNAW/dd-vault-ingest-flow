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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IngestAreaDirectoryWatcherTest {

    private final Path PATH = Path.of("target/" + this.getClass().getSimpleName() + "/inbox");


    @BeforeEach
    void setUp() throws Exception {
        FileUtils.deleteQuietly(PATH.toFile());
        Files.createDirectories(PATH);
    }

    @Test
    void start_should_callback() throws Exception {
        var watcher = new IngestAreaDirectoryWatcher(10, PATH);
        var hasBeenCalled = new boolean[]{false};
        watcher.start(p -> {
            var result = PATH.toAbsolutePath().relativize(p.toAbsolutePath());
            assertEquals(Path.of("1"), result);
            hasBeenCalled[0] = true;
        });

        var source = Path.of(getClass().getResource("/input/integration-test-complete-bag/c169676f-5315-4d86-bde0-a62dbc915228").getPath());
        Files.copy(source, PATH.resolve("1"));

        Thread.sleep(100);

        assertTrue(hasBeenCalled[0]);
    }

    @Test
    void start_should_throw_IllegalStateException_if_initial_directory_does_not_exist() {
        var watcher = new IngestAreaDirectoryWatcher(10, PATH.resolve("does-not-exist"));

        assertThrows(IllegalStateException.class, () -> {
            watcher.start(p -> {
            });
        });
    }
}