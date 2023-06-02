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
package nl.knaw.dans.vaultingest.core.deposit;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.KeyValuePair;
import nl.knaw.dans.vaultingest.core.domain.ManifestAlgorithm;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.w3c.dom.Node;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
TODO:
- add accessrights logic
- add checksum logic

 */
@ToString
@EqualsAndHashCode
@Builder
class CommonDepositFile implements DepositFile {
    private final static Pattern filenameForbidden = Pattern.compile("[:*?\"<>|;#]");
    private final static Pattern directoryLabelForbidden = Pattern.compile("[^_\\-.\\\\/ 0-9a-zA-Z]");

    private final String id;
    private final Node filesXmlNode;
    private final Node ddmNode;

    private final Path physicalPath;
    private final Map<ManifestAlgorithm, String> checksums;
    // TODO embargoes

    public String getId() {
        return id;
    }

    // TODO implement according to TRM003 and TRM004
    public boolean isRestricted() {
        if (getFilename().equals(Path.of("original-metadata.zip"))) {
            return false;
        }

        var accessibleToRights = getAccessibleToRights();
        var accessRights = getAccessRights();

        if (accessibleToRights != null) {
            return !accessibleToRights.equals("ANONYMOUS");
        }

        if (accessRights != null) {
            return accessRights.equals("OPEN_ACCESS");
        }

        return false;
    }

    @Override
    public Path getDirectoryLabel() {
        var parent = getFilePath().getParent();

        if (parent != null) {
            var sanitized = directoryLabelForbidden.matcher(parent.toString()).replaceAll("_");
            return Path.of(sanitized);
        }

        return null;
    }

    @Override
    public Path getFilename() {
        var filename = getFilePath().getFileName().toString();
        var sanitized = filenameForbidden.matcher(filename).replaceAll("_");

        return Path.of(sanitized);
    }

    @Override
    public Path getPath() {
        var directoryLabel = getDirectoryLabel();

        if (directoryLabel != null) {
            return getDirectoryLabel().resolve(getFilename());
        }

        return getFilename();
    }

    @Override
    public String getDescription() {
        var originalFilepath = getFilePath();
        var filenameWasSanitized = !getFilename().equals(originalFilepath.getFileName());
        var directoryLabelWasSanitized = getDirectoryLabel() != null && !getDirectoryLabel().equals(originalFilepath.getParent());

        var metadataFields = new HashMap<String, String>();

        var afmKeyValuePairs = XPathEvaluator.nodes(filesXmlNode, "afm:keyvaluepair")
            .map(keyValuePair -> {
                var key = XPathEvaluator.strings(keyValuePair, "afm:key").findFirst().orElse(null);
                var value = XPathEvaluator.strings(keyValuePair, "afm:value").findFirst().orElse(null);

                return new KeyValuePair(key, value);
            })
            .collect(Collectors.toList());

        var otherKeyValuePairs = XPathEvaluator.nodes(filesXmlNode, "*[not(local-name() = 'keyvaluepair')]")
            .map(keyValuePair -> {
                var key = keyValuePair.getLocalName();
                var value = keyValuePair.getTextContent();

                return new KeyValuePair(key, value);
            })
            .collect(Collectors.toList());

        // FIL002A (migration only)
        for (var item: afmKeyValuePairs) {
            metadataFields.put(item.getKey(), item.getValue());
        }

        // FIL002B (migration only)
        for (var item: otherKeyValuePairs) {
            metadataFields.put(item.getKey(), item.getValue());
        }

        // FIL003
        if (filenameWasSanitized || directoryLabelWasSanitized) {
            metadataFields.put("original_filepath", getFilePathAttribute());
        }

        // FIL004
        var description = XPathEvaluator.strings(filesXmlNode, "dcterms:description").findFirst().orElse(null);

        if (metadataFields.size() == 0 && description != null) {
            return description;
        }
        else {
            return metadataFields.entrySet().stream()
                .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("; "));
        }
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(physicalPath.toFile()));
    }

    private String getFilePathAttribute() {
        return filesXmlNode.getAttributes().getNamedItem("filepath").getTextContent();
    }

    private Path getFilePath() {
        return Path.of(getFilePathAttribute().substring("data/".length()));
    }

    private String getAccessibleToRights() {
        // TODO implement
        return null;
    }

    private String getAccessRights() {
        // TODO implement
        return null;
    }

    @Override
    public Map<ManifestAlgorithm, String> getChecksums() {
        return Collections.unmodifiableMap(checksums);
    }
}
