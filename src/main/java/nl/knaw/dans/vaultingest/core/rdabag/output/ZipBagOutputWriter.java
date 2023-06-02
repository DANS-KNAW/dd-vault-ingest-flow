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

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class ZipBagOutputWriter implements BagOutputWriter {
    private final ZipOutputStream outputStream;
    private final Path outputPath;
    private final Path workingPath;

    public ZipBagOutputWriter(Path output) throws IOException {
        this.workingPath = output.getParent().resolve(output.getFileName().toString() + ".tmp");
        removeFileIfExists(workingPath);
        // ZipOutputStream closes the underlying stream when closing, so does the BufferedOutputStream
        // so no need to individually close the streams
        this.outputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(workingPath.toFile())));
        this.outputPath = output;
    }

    @Override
    public void writeBagItem(InputStream inputStream, Path path) throws IOException {
        log.debug("Writing bag item {}", path);
        outputStream.putNextEntry(new ZipEntry(path.toString()));
        inputStream.transferTo(outputStream);
        outputStream.closeEntry();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();

        removeFileIfExists(outputPath);

        log.debug("Moving file {} to {}", workingPath, outputPath);
        Files.move(workingPath, outputPath);
    }

    void removeFileIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            log.warn("File {} already exists, removing it", path);
            Files.delete(path);
        }
    }
}
