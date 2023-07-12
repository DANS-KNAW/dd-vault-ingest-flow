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
import nl.knaw.dans.vaultingest.core.deposit.Outbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // TODO add continuePrevious to do partial batches
    public void ingest(Path inputPath, boolean isBatch, boolean continuePrevious) {
        var path = getAbsolutePath(inputPath);

        if (!path.startsWith(inboxPath)) {
            throw new IllegalArgumentException(
                String.format("Input directory must be subdirectory of %s. Provide correct absolute path or a path relative to this directory.", inboxPath));
        }

        if (isBatch) {
            log.info("Deposits found in inbox; path = {}", inputPath);
            validateBatchDirectory(path);
        }
        else {
            log.info("Deposit found in inbox; path = {}", inputPath);
            validateDepositDirectory(path);
        }

        try {
            var input = getDeposits(isBatch, path);
            var output = getOutboxPath(isBatch, path);

            for (var in : input) {
                executorService.execute(() -> depositToBagProcess.process(in, output));
            }
        }
        catch (IOException e) {
            log.error("Error while processing deposit", e);
        }
    }

    Path getAbsolutePath(Path input) {
        if (input.isAbsolute()) {
            return input;
        }

        return inboxPath.resolve(input);
    }

    List<Path> getDeposits(boolean isBatch, Path path) {
        if (!isBatch) {
            return List.of(path);
        }

        try (var subPaths = Files.list(path).filter(Files::isDirectory)) {
            return subPaths.collect(Collectors.toList());
        }
        catch (IOException e) {
            throw new IllegalArgumentException(String.format("Cannot read %s", path));
        }
    }

    Outbox getOutboxPath(boolean isBatch, Path path) throws IOException {
        if (isBatch) {
            return outbox.withBatchDirectory(path.getFileName());
        }

        return outbox;
    }

    void validateBatchDirectory(Path input) {
        if (Files.isDirectory(input)) {
            try (Stream<Path> subPaths = Files.list(input)) {
                List<Path> paths = subPaths.collect(Collectors.toList());
                for (Path f : paths) {
                    validateDepositDirectory(f);
                }
            }
            catch (IOException e) {
                throw new IllegalArgumentException(String.format("Cannot read %s", input));
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("Invalid batch: %s. At least one non-deposit: %s", input, e.getMessage()));
            }
        }
        else {
            throw new IllegalArgumentException(String.format("File %s is not a directory. Cannot be a batch.", input));
        }
    }

    void validateDepositDirectory(Path input) {
        if (!Files.isRegularFile(input.resolve("deposit.properties"))) {
            throw new IllegalArgumentException(String.format("Directory %s does not contain file deposit.properties. Not a valid deposit directory", input));
        }
    }
}
