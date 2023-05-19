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

import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.KeyValuePair;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.w3c.dom.Document;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Files extends Base {
    public static List<DepositFile> getFiles(Document ddm, Document filesXml) {
        return XPathEvaluator.nodes(filesXml, "/files:files/files:file")
            .map(node -> {
                var filepath = node.getAttributes().getNamedItem("filepath").getTextContent();

                var afmKeyValuePairs = XPathEvaluator.nodes(node, "afm:keyvaluepair")
                    .map(keyValuePair -> {
                        var key = XPathEvaluator.strings(keyValuePair, "afm:key").findFirst().orElse(null);
                        var value = XPathEvaluator.strings(keyValuePair, "afm:value").findFirst().orElse(null);

                        return new KeyValuePair(key, value);
                    })
                    .collect(Collectors.toList());

                var otherKeyValuePairs = XPathEvaluator.nodes(node, "*[not(local-name() = 'keyvaluepair')]")
                    .map(keyValuePair -> {
                        var key = keyValuePair.getLocalName();
                        var value = keyValuePair.getTextContent();

                        return new KeyValuePair(key, value);
                    })
                    .collect(Collectors.toList());

                return DepositFile.builder()
                    .id(UUID.randomUUID().toString())
                    .keyValuePairs(afmKeyValuePairs)
                    .otherMetadata(otherKeyValuePairs)
                    .filepath(filepath)
                    .build();
            })
            .collect(Collectors.toList());
    }
}
