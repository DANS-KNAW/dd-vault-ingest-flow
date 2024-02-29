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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@ToString
public class DepositOutbox implements Outbox {
    @Getter
    @AllArgsConstructor
    private enum OutboxPath {
        PROCESSED("processed"),
        REJECTED("rejected"),
        FAILED("failed");

        private final String value;
    }

    private final Path outboxPath;
    private boolean initialized = false;

    public DepositOutbox(Path outboxPath) {
        this.outboxPath = outboxPath.toAbsolutePath();
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
    public Outbox withBatchDirectory(Path subPath) {
        return new DepositOutbox(outboxPath.resolve(subPath));
    }

    @Override
    public void init(boolean allowNonEmpty) throws IOException {
        // create outbox directory if it does not exist
        log.debug("Creating directories in outbox; path = {}", outboxPath);

        var paths = new Path[] {
            this.outboxPath.resolve(OutboxPath.PROCESSED.getValue()),
            this.outboxPath.resolve(OutboxPath.FAILED.getValue()),
            this.outboxPath.resolve(OutboxPath.REJECTED.getValue())
        };

        for (var path : paths) {
            Files.createDirectories(path);
        }

        boolean dontAllowContentInDirectories = !allowNonEmpty;

        if (dontAllowContentInDirectories) {
            var nonEmptyPaths = Stream.of(paths).filter(path -> {
                    try {
                        return !directoryIsEmpty(path);
                    }
                    catch (Throwable e) {
                        throw new IllegalStateException(String.format(
                            "Failed to check if outbox %s is empty", path), e);
                    }
                })
                .toList();

            if (!nonEmptyPaths.isEmpty()) {
                throw new IllegalStateException(String.format(
                    "Outbox %s is not empty; paths containing files/directories are %s", outboxPath, nonEmptyPaths)
                );
            }
        }

        initialized = true;
    }

    private boolean directoryIsEmpty(Path path) throws IOException {
        try (var fileList = Files.list(path)) {
            return fileList.findAny().isEmpty();
        }
    }

    private void moveDepositPath(Path path, Deposit.State state) throws IOException {
        if (!initialized) {
            throw new IllegalStateException(String.format("Outbox %s not initialized; call init() first", this.outboxPath));
        }

        var outboxMapping = Map.of(
            Deposit.State.FAILED, OutboxPath.FAILED,
            Deposit.State.REJECTED, OutboxPath.REJECTED,
            Deposit.State.ACCEPTED, OutboxPath.PROCESSED
        );

        if (!outboxMapping.containsKey(state)) {
            throw new IllegalArgumentException("Unexpected state: " + state + "; only FAILED, REJECTED and ACCEPTED are allowed");
        }

        Files.move(path, outboxPath.resolve(outboxMapping.get(state).getValue()).resolve(path.getFileName()));
    }
}

