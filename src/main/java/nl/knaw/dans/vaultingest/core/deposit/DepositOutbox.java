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
package nl.knaw.dans.vaultingest.core.deposit;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class DepositOutbox implements Outbox {
    private final Path outboxPath;

    public DepositOutbox(Path outboxPath) throws IOException {
        this.outboxPath = outboxPath.toAbsolutePath();

        // create outbox directory if it does not exist
        log.info("Creating directories in outbox; path = {}", outboxPath);
        Files.createDirectories(this.outboxPath.resolve(OutboxPath.PROCESSED.getValue()));
        Files.createDirectories(this.outboxPath.resolve(OutboxPath.FAILED.getValue()));
        Files.createDirectories(this.outboxPath.resolve(OutboxPath.REJECTED.getValue()));
    }

    @Override
    public void moveDeposit(Deposit deposit) throws IOException {
        moveDepositPath(deposit.getPath(), deposit.getState());
    }

    @Override
    public void move(Path path, Deposit.State state) throws IOException {
        moveDepositPath(path, state);
    }

    @Override
    public Outbox withBatchDirectory(Path subPath) throws IOException {
        return new DepositOutbox(outboxPath.resolve(subPath));
    }

    void moveDepositPath(Path path, Deposit.State state) throws IOException {
        switch (state) {
            case FAILED:
                // move to failed
                Files.move(path, outboxPath.resolve(OutboxPath.FAILED.getValue()).resolve(path.getFileName()));
                break;
            case REJECTED:
                // move to rejected
                Files.move(path, outboxPath.resolve(OutboxPath.REJECTED.getValue()).resolve(path.getFileName()));
                break;
            case ACCEPTED:
                // move to accepted
                Files.move(path, outboxPath.resolve(OutboxPath.PROCESSED.getValue()).resolve(path.getFileName()));
                break;
            default:
                throw new IllegalArgumentException("Unexpected state: " + state + "; only FAILED, REJECTED and ACCEPTED are allowed");
        }
    }

    private enum OutboxPath {

        PROCESSED("processed"),
        REJECTED("rejected"),
        FAILED("failed"),
        ;

        private final Path value;

        OutboxPath(String value) {
            this.value = Path.of(value);
        }

        public Path getValue() {
            return value;
        }
    }
}

