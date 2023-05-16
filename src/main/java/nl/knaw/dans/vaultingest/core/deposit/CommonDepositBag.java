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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.domain.OriginalFilepaths;
import nl.knaw.dans.vaultingest.core.domain.DepositBag;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class CommonDepositBag implements DepositBag {
    private final Bag bag;
    private List<DepositFile> files;
    private OriginalFilepaths originalFilepaths;

    @Override
    public Collection<DepositFile> getPayloadFiles() {
        if (this.files == null) {
            this.files = bag.getPayLoadManifests().stream()
                .flatMap(manifest -> manifest.getFileToChecksumMap().keySet().stream())
                .map(path -> this.bag.getRootDir().relativize(path))
                .map(this::normalizePath)
                .map(path -> DepositFile.builder()
                    .path(path)
                    .id(UUID.randomUUID().toString())
                    .build())
                .collect(Collectors.toList());
        }

        return this.files;
    }

    @Override
    public InputStream inputStreamForPayloadFile(DepositFile depositFile) {
        try {
            var paths = this.getOriginalFilepaths();
            var path = paths.getPhysicalPath(depositFile.getPath());

            return new FileInputStream(bag.getRootDir().resolve(path).toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<Path> getMetadataFiles() throws IOException {
        try (var list = Files.list(this.bag.getRootDir().resolve("metadata"))) {
            return list
                .map(path -> this.bag.getRootDir().relativize(path))
                .collect(Collectors.toList());
        }
    }

    @Override
    public InputStream inputStreamForMetadataFile(Path path) {
        try {
            return new FileInputStream(bag.getRootDir().resolve(path).toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getBagInfoFile() {
        try {
            return new FileInputStream(bag.getRootDir().resolve("bag-info.txt").toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getBagItFile() {
        try {
            return new FileInputStream(bag.getRootDir().resolve("bagit.txt").toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getMetadataValue(String key) {
        var value = this.bag.getMetadata().get(key);
        return value != null ? value : List.of();
    }

    private Path normalizePath(Path path) {
        var mappings = this.getOriginalFilepaths();
        return mappings.getLogicalPath(path);
    }

    // TODO think about where to place this
    OriginalFilepaths getOriginalFilepaths() {
        if (this.originalFilepaths == null) {
            this.originalFilepaths = new OriginalFilepaths();

            try {
                var path = this.bag.getRootDir().resolve("original-filepaths.txt");

                if (Files.exists(path)) {
                    try (var lines = Files.lines(path)) {
                        lines.filter(StringUtils::isNotBlank)
                            .map(line -> line.split("\\s+", 2))
                            .forEach(line -> this.originalFilepaths.addMapping(
                                Path.of(line[1]), Path.of(line[0]))
                            );
                    }
                }
            } catch (IOException e) {
                log.error("Could not read original-file-paths.txt", e);
                // TODO: throw exception?
            }
        }

        return this.originalFilepaths;
    }
}
