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

import nl.knaw.dans.vaultingest.core.domain.ids.DAI;
import nl.knaw.dans.vaultingest.core.domain.ids.ISNI;
import nl.knaw.dans.vaultingest.core.domain.ids.ORCID;
import nl.knaw.dans.vaultingest.core.domain.ids.VIAF;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.w3c.dom.Node;

public class Base {

    static String getFirstValue(Node node, String expression) {
        return XPathEvaluator.strings(node, expression).map(String::trim).findFirst().orElse(null);
    }

    static DAI getDAI(Node node) {
        return XPathEvaluator.strings(node, "dcx-dai:DAI").map(DAI::new).findFirst().orElse(null);
    }

    static ISNI getISNI(Node node) {
        return XPathEvaluator.strings(node, "dcx-dai:ISNI").map(ISNI::new).findFirst().orElse(null);
    }

    static ORCID getORCID(Node node) {
        return XPathEvaluator.strings(node, "dcx-dai:ORCID").map(ORCID::new).findFirst().orElse(null);
    }

    static VIAF getVIAF(Node node) {
        return XPathEvaluator.strings(node, "dcx-dai:VIAF").map(VIAF::new).findFirst().orElse(null);
    }
}
