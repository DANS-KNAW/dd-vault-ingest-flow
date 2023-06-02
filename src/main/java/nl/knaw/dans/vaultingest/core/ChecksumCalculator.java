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
package nl.knaw.dans.vaultingest.core;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.domain.ManifestAlgorithm;
import org.apache.commons.io.output.NullOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ChecksumCalculator {

    public Map<ManifestAlgorithm, String> calculateChecksums(InputStream inputStream, Collection<ManifestAlgorithm> algorithms) {
        var result = new HashMap<ManifestAlgorithm, String>();

        var output = (OutputStream) NullOutputStream.NULL_OUTPUT_STREAM;
        var streams = new HashMap<ManifestAlgorithm, DigestOutputStream>();

        for (var alg: algorithms) {
            output = new DigestOutputStream(output, alg.getMessageDigestInstance());
            streams.put(alg, (DigestOutputStream) output);
        }

        try {
            inputStream.transferTo(output);
        }
        catch (IOException e) {
            log.error("Error calculating checksums", e);
        }

        for (var entry: streams.entrySet()) {
            result.put(entry.getKey(), bytesToHex(entry.getValue().getMessageDigest().digest()));
        }

        return result;
    }

    private String bytesToHex(byte[] digest) {
        var sb = new StringBuilder();
        for (var b: digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
