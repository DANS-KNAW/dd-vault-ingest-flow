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

import nl.knaw.dans.vaultingest.core.DepositToBagProcess;
import nl.knaw.dans.vaultingest.core.domain.Outbox;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

class AutoIngestAreaTest {

    @Test
    void watcher_should_call_callback() throws Exception {
        final Thread[] thread = {null};

        var executorService = Executors.newSingleThreadExecutor(r -> {
            thread[0] = new Thread(r);
            return thread[0];
        });

        var process = Mockito.mock(DepositToBagProcess.class);
        var outbox = Mockito.mock(Outbox.class);

        var area = new AutoIngestArea(
            executorService,
            callback -> callback.onItemCreated(Path.of("fake/path")),
            process,
            outbox
        );

        area.start();

        // wait for thread to finish
        while (true) {
            try {
                thread[0].join(10);
                break;
            }
            catch (InterruptedException e) {
                // noop
            }

            Thread.sleep(10);
        }

        Mockito.verify(process).process(Path.of("fake/path"), outbox);

    }

}