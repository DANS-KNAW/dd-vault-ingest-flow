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
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.validator.InvalidDepositException;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
class ProcessDepositTask implements Runnable {
    private final CommonDepositFactory depositFactory;
    private final DepositToBagProcess depositToBagProcess;
    private final Path path;
    private final Path outboxPath;

    ProcessDepositTask(CommonDepositFactory depositFactory, DepositToBagProcess depositToBagProcess, Path path, Path outboxPath) {
        this.depositFactory = depositFactory;
        this.depositToBagProcess = depositToBagProcess;
        this.path = path;
        this.outboxPath = outboxPath;
    }

    @Override
    public void run() {
        try {
            var deposit = depositFactory.loadDeposit(path);
            depositToBagProcess.process(deposit);
            depositFactory.saveDeposit(path);

            log.info("Deposit {} processed successfully", deposit.getId());
            moveDeposit(path, outboxPath.resolve("processed"));
        }
        catch (InvalidDepositException e) {
            log.error("Deposit on path {} was rejected", path, e);
            moveDeposit(path, outboxPath.resolve("rejected"));
        }
        catch (Throwable e) {
            log.error("Deposit on path {} failed", path, e);
            moveDeposit(path, outboxPath.resolve("failed"));
        }
    }

    private void moveDeposit(Path path, Path outboxPath) {
        try {
            Files.move(path, outboxPath.resolve(path.getFileName()));
        }
        catch (Exception e) {
            log.error("Unable to move deposit {} to outbox with path {}", path, outboxPath, e);
            throw new RuntimeException(e);
        }
    }
}
