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
package nl.knaw.dans.vaultingest.core.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
/*
Todo:
- fix the whole path thing, it should be clear what the difference is between a physical path (not regarding original-filepaths.txt) and the logical name for the RDF output
  - perhaps naming them differently would help
  - path1 is the path found in the @filepath attribute
  - path2 is the one that should appear in the RDF, which is stripped of special characters and the data/ prefix is gone
- add accessrights logic
- add checksum logic

 */
@ToString
@EqualsAndHashCode
@Builder
public class DepositFile {
    private final static Pattern filenameForbidden = Pattern.compile("[:*?\"<>|;#]");
    private final static Pattern directoryLabelForbidden = Pattern.compile("[^_\\-.\\\\/ 0-9a-zA-Z]");

    private final String id;
    private final String filepath;
    private final List<KeyValuePair> keyValuePairs;
    private final List<KeyValuePair> otherMetadata;
    private final String description;
    private final String accessibleToRights;
    private final String accessRights;
    // TODO also store checksums here?
    // TODO embargoes

    public String getId() {
        return id;
    }


    public Path getOriginalPath() {
        return Path.of(filepath);
    }

    // TODO implement according to TRM003 and TRM004
    public boolean isRestricted() {
        if (getFilename().equals(Path.of("original-metadata.zip"))) {
            return false;
        }

        if (accessibleToRights != null) {
            return !accessibleToRights.equals("ANONYMOUS");
        }

        if (accessRights != null) {
            return accessRights.equals("OPEN_ACCESS");
        }

        return false;
    }

    public Path getDirectoryLabel() {
        var parent = getFilePath().getParent();

        if (parent != null) {
            var sanitized = directoryLabelForbidden.matcher(parent.toString()).replaceAll("_");
            return Path.of(sanitized);
        }

        return null;
    }

    public Path getFilename() {
        var filename = getFilePath().getFileName().toString();
        var sanitized = filenameForbidden.matcher(filename).replaceAll("_");

        return Path.of(sanitized);
    }

    public Path getPath() {
        return Path.of(filepath);
    }

    public Path getRelativePath() {
        var directoryLabel = getDirectoryLabel();

        if (directoryLabel != null) {
            return getDirectoryLabel().resolve(getFilename());
        }

        return getFilename();
    }

    public String getDescription() {
        var originalFilepath = getFilePath();
        var filenameWasSanitized = !getFilename().equals(originalFilepath.getFileName());
        var directoryLabelWasSanitized = getDirectoryLabel() != null && !getDirectoryLabel().equals(originalFilepath.getParent());

        var metadataFields = new HashMap<String, String>();

        // FIL002A (migration only)
        if (keyValuePairs != null) {
            for (var item : keyValuePairs) {
                metadataFields.put(item.getKey(), item.getValue());
            }
        }

        // FIL002B (migration only)
        if (otherMetadata != null) {
            for (var item : otherMetadata) {
                metadataFields.put(item.getKey(), item.getValue());
            }
        }

        // FIL003
        if (filenameWasSanitized || directoryLabelWasSanitized) {
            metadataFields.put("original_filepath", filepath);
        }

        // FIL004
        if (metadataFields.size() == 0 && description != null) {
            return description;
        }
        else {
            return metadataFields.entrySet().stream()
                .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("; "));
        }
    }

    private Path getFilePath() {
        return Path.of(filepath.substring("data/".length()));
    }
}
