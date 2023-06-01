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
import nl.knaw.dans.vaultingest.core.DepositToBagProcess;
import nl.knaw.dans.vaultingest.core.deposit.CommonDepositFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

@Slf4j
public class MigrationIngestArea {
    private final ExecutorService executorService;
    private final CommonDepositFactory depositFactory;
    private final DepositToBagProcess depositToBagProcess;
    private final Path inboxPath;
    private final Path outboxPath;

    public MigrationIngestArea(ExecutorService executorService, CommonDepositFactory depositFactory, DepositToBagProcess depositToBagProcess, Path inboxPath, Path outboxPath)
        throws IOException {
        this.executorService = executorService;
        this.depositFactory = depositFactory;
        this.depositToBagProcess = depositToBagProcess;
        this.inboxPath = inboxPath.toAbsolutePath();
        this.outboxPath = outboxPath.toAbsolutePath();

        this.start();
    }

    public void ingest(Path depositPath) {
        var path = depositPath.toAbsolutePath();

        // FIXME the current ingestflow has support for batches too, this is not currently implemented
        if (!path.startsWith(inboxPath)) {
            throw new IllegalArgumentException(
                String.format("Input directory must be subdirectory of %s. Provide correct absolute path or a path relative to this directory.", inboxPath));
        }

        log.info("Deposit found in inbox; path = {}", depositPath);
        var task = new ProcessDepositTask(depositFactory, depositToBagProcess, depositPath, outboxPath);
        executorService.execute(task);
    }

    void start() throws IOException {
        log.info("Creating directories in outbox; path = {}", outboxPath);
        Files.createDirectories(outboxPath);
        Files.createDirectories(outboxPath.resolve("processed"));
        Files.createDirectories(outboxPath.resolve("rejected"));
        Files.createDirectories(outboxPath.resolve("failed"));
    }
}
