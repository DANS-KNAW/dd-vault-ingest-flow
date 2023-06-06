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
import nl.knaw.dans.vaultingest.core.domain.Outbox;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

@Slf4j
public class MigrationIngestArea {
    private final ExecutorService executorService;
    private final DepositToBagProcess depositToBagProcess;
    private final Path inboxPath;
    private final Outbox outbox;

    public MigrationIngestArea(
        ExecutorService executorService,
        DepositToBagProcess depositToBagProcess,
        Path inboxPath,
        Outbox outbox
    ) {
        this.executorService = executorService;
        this.depositToBagProcess = depositToBagProcess;
        this.inboxPath = inboxPath.toAbsolutePath();
        this.outbox = outbox;
    }

    public void ingest(Path depositPath) {
        var path = depositPath.toAbsolutePath();

        // FIXME the current ingestflow has support for batches too, this is not currently implemented
        if (!path.startsWith(inboxPath)) {
            throw new IllegalArgumentException(
                String.format("Input directory must be subdirectory of %s. Provide correct absolute path or a path relative to this directory.", inboxPath));
        }

        log.info("Deposit found in inbox; path = {}", depositPath);

        executorService.execute(() -> {
            depositToBagProcess.process(depositPath, outbox);
        });
    }
}
