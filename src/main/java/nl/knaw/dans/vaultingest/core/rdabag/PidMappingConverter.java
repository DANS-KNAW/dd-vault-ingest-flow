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
package nl.knaw.dans.vaultingest.core.rdabag;

import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.PidMappings;

class PidMappingConverter {

    PidMappings convert(Deposit deposit) {

        var mappings = new PidMappings();
        // does not include the "title of the deposit" as a mapping
        mappings.addMapping(deposit.getId(), "data/");

        var bag = deposit.getBag();

        for (var file : bag.getPayloadFiles()) {
            mappings.addMapping("file:///" + file.getId(), file.getPath().toString());
        }

        return mappings;
    }
}
