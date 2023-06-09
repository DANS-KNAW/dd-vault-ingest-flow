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
package nl.knaw.dans.vaultingest.core.utilities;

import lombok.Builder;
import lombok.Data;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.ManifestAlgorithm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

@Data
@Builder
public class TestDepositFile implements DepositFile {
    private final String id;
    private final boolean restricted;
    private final Path path;
    private final String description;
    private final Map<ManifestAlgorithm, String> checksums;

    @Override
    public Path getDirectoryLabel() {
        return path.getParent();
    }

    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(("input for file " + id).getBytes());
    }
}
