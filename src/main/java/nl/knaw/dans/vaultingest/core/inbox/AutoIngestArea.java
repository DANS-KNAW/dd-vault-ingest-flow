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

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

@Slf4j
public class AutoIngestArea {
    private final ExecutorService executorService;
    // TODO extract interface and use that
    private final ProcessDepositTaskFactory processDepositTaskFactory;
    private final IngestAreaDirectoryWatcher ingestAreaDirectoryWatcher;
    private final Path outboxPath;

    public AutoIngestArea(ExecutorService executorService, ProcessDepositTaskFactory processDepositTaskFactory, IngestAreaDirectoryWatcher ingestAreaDirectoryWatcher, Path outboxPath)
        throws IOException {
        this.executorService = executorService;
        this.processDepositTaskFactory = processDepositTaskFactory;
        this.ingestAreaDirectoryWatcher = ingestAreaDirectoryWatcher;
        this.outboxPath = outboxPath.toAbsolutePath();
    }

    public void start() throws IOException {
        log.info("Creating directories in outbox; path = {}", outboxPath);
        Files.createDirectories(outboxPath);
        Files.createDirectories(outboxPath.resolve("processed"));
        Files.createDirectories(outboxPath.resolve("rejected"));
        Files.createDirectories(outboxPath.resolve("failed"));

        ingestAreaDirectoryWatcher.start((path) -> {
            log.info("New item in inbox; path = {}", path);
            executorService.submit(processDepositTaskFactory.createProcessDepositTask(path, outboxPath));
        });
    }

}
