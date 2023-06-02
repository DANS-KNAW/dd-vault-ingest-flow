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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

class AutoIngestAreaTest {

    @Test
    void start_should_create_directories() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        var processDepositTaskFactory = Mockito.mock(ProcessDepositTaskFactory.class);
        var watcher = Mockito.mock(IngestAreaDirectoryWatcher.class);
        var path = Path.of("some/outbox");

        try (var files = Mockito.mockStatic(Files.class)) {
            var area = new AutoIngestArea(
                executor,
                processDepositTaskFactory,
                watcher,
                path
            );

            area.start();

            files.verify(() -> Files.createDirectories(path.toAbsolutePath()));
            files.verify(() -> Files.createDirectories(path.toAbsolutePath().resolve("rejected")));
            files.verify(() -> Files.createDirectories(path.toAbsolutePath().resolve("processed")));
            files.verify(() -> Files.createDirectories(path.toAbsolutePath().resolve("failed")));
        }

    }

}