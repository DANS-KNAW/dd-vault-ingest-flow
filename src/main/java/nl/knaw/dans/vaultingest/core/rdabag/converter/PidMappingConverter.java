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
package nl.knaw.dans.vaultingest.core.rdabag.converter;

import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.PidMappings;

public class PidMappingConverter {

    public PidMappings convert(Deposit deposit) {
        var dataPath = "data/";
        var mappings = new PidMappings();

        // does not include the "title of the deposit" as a mapping
        mappings.addMapping(deposit.getDoi(), dataPath);

        for (var file : deposit.getPayloadFiles()) {
            var path = file.getPath().toString();
            mappings.addMapping("file:///" + file.getId(), path);
        }

        return mappings;
    }
}
