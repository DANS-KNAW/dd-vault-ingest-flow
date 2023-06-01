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

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.reader.BagReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
class CommonDepositBag {
    private final Path bagDir;
    private Bag bag;

    public Collection<Path> getMetadataFiles() throws IOException {
        try (var list = Files.list(this.bagDir.resolve("metadata"))) {
            return list
                .map(this.bagDir::relativize)
                .collect(Collectors.toList());
        }
    }

    public InputStream inputStreamForMetadataFile(Path path) {
        try {
            return new FileInputStream(bagDir.resolve(path).toFile());
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getMetadataValue(String key) {
        var bag = getBag();
        var value = bag.getMetadata().get(key);
        return value != null ? value : List.of();
    }

    private Bag getBag() {
        if (this.bag == null) {
            try {
                this.bag = new BagReader().read(this.bagDir);
            }
            catch (IOException |
                   InvalidBagitFileFormatException |
                   UnsupportedAlgorithmException |
                   MaliciousPathException |
                   UnparsableVersionException e) {
                throw new RuntimeException(e);
            }
        }

        return this.bag;
    }

    Path getBagDir() {
        return bagDir;
    }
}
