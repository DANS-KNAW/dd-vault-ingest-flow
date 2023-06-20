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

class AutoIngestAreaTest {

    @Test
    void watcher_should_call_callback() {
        var process = Mockito.mock(DepositToBagProcess.class);
        var outbox = Mockito.mock(Outbox.class);

        var area = new AutoIngestArea(
            // just run it in the current thread for testing purposes
            Runnable::run,
            callback -> callback.onItemCreated(Path.of("fake/path")),
            process,
            outbox
        );

        area.start();

        Mockito.verify(process).process(Path.of("fake/path"), outbox);
    }
}