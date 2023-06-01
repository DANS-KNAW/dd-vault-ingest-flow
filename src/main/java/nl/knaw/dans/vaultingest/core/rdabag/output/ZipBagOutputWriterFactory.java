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
package nl.knaw.dans.vaultingest.core.rdabag.output;

import nl.knaw.dans.vaultingest.core.domain.Deposit;

import java.io.IOException;
import java.nio.file.Path;

public class ZipBagOutputWriterFactory implements BagOutputWriterFactory {
    private final Path outputDir;

    public ZipBagOutputWriterFactory(Path outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public BagOutputWriter createBagOutputWriter(Deposit deposit) throws IOException {
        // TODO version should be coming from the deposit
        var output = outputDir.resolve(outputFilename(deposit.getDoi(), "1.0"));
        return new ZipBagOutputWriter(output);
    }

    private Path outputFilename(String doi, String version) {
        doi = doi.replaceAll("[^a-zA-Z0-9]", "-");
        return Path.of(String.format("%s-v%s.zip", doi, version).toLowerCase());
    }
}
