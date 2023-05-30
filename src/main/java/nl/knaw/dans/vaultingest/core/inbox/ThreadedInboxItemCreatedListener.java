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
import nl.knaw.dans.vaultingest.core.deposit.CommonDepositFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

public class ThreadedInboxItemCreatedListener implements InboxItemCreatedListener {
    private final ExecutorService executorService;
    // TODO extract interface and use that, also make it a Manager (or something) that has a move() method
    private final CommonDepositFactory depositFactory;
    private final DepositToBagProcess depositToBagProcess;
    private final Path outboxPath;

    public ThreadedInboxItemCreatedListener(ExecutorService executorService, CommonDepositFactory depositFactory, DepositToBagProcess depositToBagProcess, Path outboxPath) {
        this.executorService = executorService;
        this.depositFactory = depositFactory;
        this.depositToBagProcess = depositToBagProcess;
        this.outboxPath = outboxPath;
    }

    @Override
    public void onInboxItemCreated(Path path) {
        var task = new ProcessDepositTask(path, outboxPath);
        executorService.execute(task);
    }

    private class ProcessDepositTask implements Runnable {
        private final Path path;
        private final Path outboxPath;


        private ProcessDepositTask(Path path, Path outboxPath) {
            this.path = path;
            this.outboxPath = outboxPath;
        }

        @Override
        public void run() {
            try {
                var deposit = depositFactory.loadDeposit(path);
                depositToBagProcess.process(deposit);

                Files.move(path, outboxPath.resolve(path.getFileName()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
