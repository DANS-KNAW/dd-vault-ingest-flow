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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class ZipBagOutputWriter implements BagOutputWriter {

    private final ZipOutputStream outputStream;

    public ZipBagOutputWriter(Path output) throws FileNotFoundException {
        this.outputStream = new ZipOutputStream(new FileOutputStream(output.toFile()));
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
    }
}
