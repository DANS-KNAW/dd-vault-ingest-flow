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
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.nio.file.Path;

@Slf4j
public class IngestAreaListener {
    private final long pollingInterval;
    private final IngestAreaItemCreated ingestAreaItemCreated;

    public IngestAreaListener(long pollingIntervalMilliseconds, IngestAreaItemCreated ingestAreaItemCreated) {
        this.pollingInterval = pollingIntervalMilliseconds;
        this.ingestAreaItemCreated = ingestAreaItemCreated;
    }

    void start(Path inboxDir) {
        var filter = FileFilterUtils.and(
            FileFilterUtils.directoryFileFilter(),
            FileFilterUtils.asFileFilter(f -> f.getParentFile().equals(inboxDir.toFile()))
        );

        var observer = new FileAlterationObserver(inboxDir.toFile(), filter);
        observer.addListener(new EventHandler());
        var monitor = new FileAlterationMonitor(pollingInterval);
        monitor.addObserver(observer);

        try {
            log.debug("Starting FileAlterationMonitor for directory {}", inboxDir);
            monitor.start();
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Could not start monitoring %s", inboxDir), e);
        }
    }

    private class EventHandler extends FileAlterationListenerAdaptor {

        @Override
        public void onStart(FileAlterationObserver observer) {
            // TODO implement the duplicate logic
        }

        @Override
        public void onDirectoryCreate(File directory) {
            ingestAreaItemCreated.onItemCreated(directory.toPath());
        }
    }
}
