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
package nl.knaw.dans.vaultingest.core.mappings;

import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.deposit.PayloadFile;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DVCore;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.w3c.dom.Document;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Terms extends Base {

    public static List<Statement> toRDF(Resource resource, Deposit deposit) {

        return toComplexTerms(resource, DVCore.fileTermsOfAccess, List.of(deposit), (element, value) -> {
            var requestAccess = isRequestAccess(deposit.getDdm(), deposit.getPayloadFiles()) ? "Yes" : "No";
            element.addProperty(DVCore.fileRequestAccess, requestAccess);

            if ("No".equals(requestAccess)) {
                var terms = getTermsOfAccess(deposit.getDdm(), deposit.getPayloadFiles());

                if (terms != null) {
                    element.addProperty(DVCore.termsOfAccess, getTermsOfAccess(deposit.getDdm(), deposit.getPayloadFiles()));
                }
            }
        });
    }

    // false = no, true = yes
    static boolean isRequestAccess(Document ddm, Collection<PayloadFile> files) {
        // TRM002
        var containsNone = false;

        for (var file : files) {
            var accessibleToRights = DataFile.getAccessibleToRights(file.getFilesXmlNode());

            if ("NONE".equals(accessibleToRights)) {
                containsNone = true;
                break;
            }
        }

        if (containsNone) {
            return false;
        }

        // TRM003
        var accessRights = DataFile.getAccessRights(ddm);

        return !"NO_ACCESS".equals(accessRights);

        // TRM004
    }

    static String getTermsOfAccess(Document ddm, Collection<PayloadFile> files) {
        // TRM005
        var containsNone = false;

        for (var file : files) {
            var accessRights = DataFile.getAccessibleToRights(file.getFilesXmlNode());

            if ("NONE".equals(accessRights)) {
                containsNone = true;
                break;
            }
        }

        var accessRights = XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcterms:accessRights")
            .map(String::trim)
            .findFirst()
            .orElse(null);

        if (containsNone) {
            return accessRights != null ? accessRights : "N/a";
        }

        // TRM006
        var knownOrRestrictive = Set.of("RESTRICTED_REQUEST", "KNOWN");

        var accessibleToRights = files.stream()
            .map(f -> DataFile.getAccessibleToRights(f.getFilesXmlNode()))
            .filter(Objects::nonNull)
            .anyMatch(knownOrRestrictive::contains);

        if (accessibleToRights) {
            return accessRights != null ? accessRights : "";
        }

        return null;
    }
}