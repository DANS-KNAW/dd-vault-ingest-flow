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

import io.dropwizard.lifecycle.Managed;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.ConvertToRdaBagTaskFactory;
import nl.knaw.dans.vaultingest.core.deposit.Outbox;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

@Slf4j
@AllArgsConstructor
public class AutoIngestArea implements Managed {
    private final ExecutorService executorService;
    private final IngestAreaWatcher ingestAreaWatcher;
    private final ConvertToRdaBagTaskFactory convertToRdaBagTaskFactory;
    private final Outbox outbox;

    @Override
    public void start() {
        log.info("Starting AutoIngestArea for outbox {}", outbox);
        try {
            outbox.init(true);
            log.debug("Initializing outbox {}", outbox);

            ingestAreaWatcher.start((path) -> {
                log.debug("New item in inbox; path = {}", path);
                executorService.execute(convertToRdaBagTaskFactory.create(path, outbox));
            });
        }
        catch (IOException e) {
            log.error("Error while starting the ingest area watcher for outbox {}", outbox, e);
            throw new IllegalStateException("Error while starting the ingest area watcher for outbox " + outbox, e);
        }
    }
}
