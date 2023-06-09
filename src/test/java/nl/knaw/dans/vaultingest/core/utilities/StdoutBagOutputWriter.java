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

import nl.knaw.dans.vaultingest.core.rdabag.output.BagOutputWriter;

import java.io.InputStream;
import java.nio.file.Path;

public class StdoutBagOutputWriter implements BagOutputWriter {

    @Override
    public void writeBagItem(InputStream inputStream, Path path) {
        System.out.println("--- START(" + path + ") ---");

        try {
            // only output first 512 bytes
                        byte[] bytes = new byte[64];
                        inputStream.read(bytes);
                        System.out.write(bytes);
                        System.out.println("...");
//            inputStream.transferTo(System.out);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n--- END(" + path + ") ---");
    }

    @Override
    public void close() {
        // noop
    }
}
