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
package nl.knaw.dans.vaultingest.core.rdabag.serializer;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.domain.Deposit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class OriginalMetadataSerializer {
    public InputStream serialize(Deposit deposit) throws IOException {
        // this is all the files in the metadata/ folder. If bagit.txt and bag-info.txt also need to be zipped, they need to
        // be explicitly added here
        var files = deposit.getMetadataFiles();
        var output = new ByteArrayOutputStream();

        try (var outputStream = new ZipOutputStream(output)) {
            for (var file: files) {
                log.debug("Writing metadata file {}", file);
                outputStream.putNextEntry(new ZipEntry(file.toString()));

                try (var inputStream = deposit.inputStreamForMetadataFile(file)) {
                    inputStream.transferTo(outputStream);
                }

                outputStream.closeEntry();
            }
        }

        var outputBytes = output.toByteArray();
        log.debug("Serialized original metadata to {} bytes", outputBytes.length);

        return new ByteArrayInputStream(outputBytes);
    }
}
