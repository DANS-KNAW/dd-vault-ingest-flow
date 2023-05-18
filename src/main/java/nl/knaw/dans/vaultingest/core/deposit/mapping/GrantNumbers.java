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
package nl.knaw.dans.vaultingest.core.deposit.mapping;

import nl.knaw.dans.vaultingest.core.domain.metadata.GrantNumber;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.w3c.dom.Node;

import java.util.List;
import java.util.stream.Collectors;

public class GrantNumbers extends Base {
    public static List<GrantNumber> getGrantNumbers(Node node) {
        // TODO CIT023 is under revision
        // CIT024
        return XPathEvaluator.strings(node,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:identifier[@xsi:type = 'id-type:NWO-PROJECTNR']")
            .map(value -> GrantNumber.builder()
                .agency("NWO")
                .value(value)
                .build())
            .collect(Collectors.toList());
    }
}
