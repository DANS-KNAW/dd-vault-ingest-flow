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

import java.util.concurrent.ExecutorService;

@Slf4j
public class AutoIngestArea {
    private final ExecutorService executorService;
    private final IngestAreaDirectoryWatcher ingestAreaDirectoryWatcher;
    private final DepositToBagProcess depositToBagProcess;
    private final Outbox outbox;

    public AutoIngestArea(
        ExecutorService executorService,
        IngestAreaDirectoryWatcher ingestAreaDirectoryWatcher,
        DepositToBagProcess depositToBagProcess,
        Outbox outbox
    ) {
        this.executorService = executorService;
        this.ingestAreaDirectoryWatcher = ingestAreaDirectoryWatcher;
        this.depositToBagProcess = depositToBagProcess;
        this.outbox = outbox;
    }

    public void start() {
        ingestAreaDirectoryWatcher.start((path) -> {
            log.info("New item in inbox; path = {}", path);

            executorService.submit(() -> depositToBagProcess.process(path, outbox));
        });
    }

}
