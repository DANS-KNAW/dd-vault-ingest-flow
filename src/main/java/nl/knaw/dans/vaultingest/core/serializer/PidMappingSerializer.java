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
package nl.knaw.dans.vaultingest.core.serializer;

import nl.knaw.dans.vaultingest.core.domain.PidMappings;

import java.io.IOException;
import java.io.OutputStream;

public class PidMappingSerializer {

    public void serialize(PidMappings mappings, OutputStream outputStream) throws IOException {
        var str = new StringBuilder();

        for (var mapping : mappings.getPidMappings()) {
            str.append(String.format("%s %s\n", mapping.getId(), mapping.getPath()));
        }

        outputStream.write(str.toString().getBytes());
    }
}