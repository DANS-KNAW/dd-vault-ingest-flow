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

import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetOrganization;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.w3c.dom.Document;

import java.util.List;
import java.util.stream.Collectors;

public class Organizations extends Base {

    public static List<DatasetOrganization> getOrganizations(Document ddm) {
        return XPathEvaluator.nodes(ddm,
                "/ddm:DDM/ddm:profile/dcx-dai:creatorDetails/dcx-dai:organization")
            .map(node -> DatasetOrganization.builder()
                .name(getFirstValue(node, "dcx-dai:name"))
                .isni(getISNI(node))
                .viaf(getVIAF(node))
                .build())
            .collect(Collectors.toList());
    }
}
