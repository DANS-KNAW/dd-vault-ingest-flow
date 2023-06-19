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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZipBagOutputWriterTest {

    Path OUTPUT = Path.of("target/test", getClass().getSimpleName(), "output.zip");

    @BeforeEach
    void setUp() throws IOException {
        FileUtils.deleteQuietly(OUTPUT.toFile());
        Files.createDirectories(OUTPUT.getParent());
    }

    @Test
    void writeBagItem() throws Exception {
        try (var writer = new ZipBagOutputWriter(OUTPUT)) {
            writer.writeBagItem(new ByteArrayInputStream("test".getBytes()), Path.of("test.txt"));
            writer.writeBagItem(new ByteArrayInputStream("in a folder".getBytes()), Path.of("a/folder/test.txt"));
            writer.writeBagItem(new ByteArrayInputStream("something else".getBytes()), Path.of("/no_extension"));
        }

        var entries = new HashMap<String, String>();

        try (var zip = new ZipFile(OUTPUT.toFile())) {
            zip.stream()
                .forEach(entry -> {
                    var output = new ByteArrayOutputStream();

                    try {
                        zip.getInputStream(entry).transferTo(output);
                        entries.put(entry.getName(), output.toString());
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        }

        assertEquals("test", entries.get("test.txt"));
        assertEquals("in a folder", entries.get("a/folder/test.txt"));
        assertEquals("something else", entries.get("/no_extension"));
    }
}