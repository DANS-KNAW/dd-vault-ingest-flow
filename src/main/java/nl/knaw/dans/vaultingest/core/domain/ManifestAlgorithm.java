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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public enum ManifestAlgorithm {
    MD5("MD5"),
    SHA1("SHA-1"),
    ;

    private final String name;

    ManifestAlgorithm(String s) {
        this.name = s;
    }

    public static ManifestAlgorithm from(String messageDigestName) throws NoSuchAlgorithmException {
        var sanitized = messageDigestName.replaceAll("-", "").toLowerCase();

        for (ManifestAlgorithm algorithm: ManifestAlgorithm.values()) {
            if (algorithm.getName().equals(sanitized)) {
                return algorithm;
            }
        }

        throw new NoSuchAlgorithmException("Unknown message digest algorithm: " + messageDigestName);
    }

    public MessageDigest getMessageDigestInstance() {
        try {
            // MD5 is not required to be implemented
            return MessageDigest.getInstance(name);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithm " + name + " is not available", e);
        }
    }

    public String getName() {
        return name.replaceAll("-", "").toLowerCase();
    }
}
