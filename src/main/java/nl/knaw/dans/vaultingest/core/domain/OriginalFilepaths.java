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
package nl.knaw.dans.vaultingest.core.domain;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@ToString
public class OriginalFilepaths {
    private final List<Mapping> mappings = new ArrayList<>();

    public Path getLogicalPath(Path physicalPath) {
        // return the logical path if there is a mapping for the given path
        // otherwise, just the path
        return mappings.stream()
            .filter(mapping -> mapping.getPhysicalPath().equals(physicalPath))
            .map(Mapping::getLogicalPath)
            .findFirst()
            .orElse(physicalPath);
    }

    public Path getPhysicalPath(Path logicalPath) {
        // return the physical path if there is a mapping for the given path
        // otherwise, just the path
        // note this does not check if paths exist
        return mappings.stream()
            .filter(mapping -> mapping.getLogicalPath().equals(logicalPath))
            .map(Mapping::getPhysicalPath)
            .findFirst()
            .orElse(logicalPath);
    }

    public void addMapping(Path logicalPath, Path physicalPath) {
        mappings.add(new Mapping(logicalPath, physicalPath));
    }

    @AllArgsConstructor
    @ToString
    static class Mapping {
        private final Path logicalPath;
        private final Path physicalPath;

        public Path getLogicalPath() {
            return logicalPath;
        }

        public Path getPhysicalPath() {
            if (physicalPath != null) {
                return physicalPath;
            }

            return logicalPath;
        }
    }
}
