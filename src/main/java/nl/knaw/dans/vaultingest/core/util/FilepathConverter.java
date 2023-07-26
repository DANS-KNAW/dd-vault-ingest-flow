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
package nl.knaw.dans.vaultingest.core.util;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

public class FilepathConverter {
    // It is very important that the % character is processed first, as it is also
    // used to encode the other characters.
    private final static String ESCAPE_CHARACTERS = "%\r\n";

    /**
     * Replaces invalid path characters with percent-encoded hex values. This is a requirement from the bagit specification.
     *
     * The Bagit spec says: If a _filepath_ includes a Line Feed (LF), a Carriage Return (CR), a Carriage-Return Line Feed (CRLF), or a percent sign (%), those characters (and only those) MUST be
     * percent-encoded following [RFC3986].
     *
     * RFC3986 says: A percent-encoding mechanism is used to represent a data octet in a component when that octet's corresponding character is outside the allowed set or is being used as a delimiter
     * of, or within, the component.  A percent-encoded octet is encoded as a character triplet, consisting of the percent character "%" followed by the two hexadecimal digits representing that
     * octet's numeric value.  For example, "%20" is the percent-encoding for the binary octet "00100000" (ABNF: %x20), which in US-ASCII corresponds to the space character (SP).  Section 2.4
     * describes when percent-encoding and decoding is applied.
     *
     * pct-encoded = "%" HEXDIG HEXDIG
     *
     * The uppercase hexadecimal digits 'A' through 'F' are equivalent to the lowercase digits 'a' through 'f', respectively.  If two URIs differ only in the case of hexadecimal digits used in
     * percent-encoded octets, they are equivalent.  For consistency, URI producers and normalizers should use uppercase hexadecimal digits for all percent- encodings.
     *
     * @param path
     * @return
     */
    public static String convertFilepath(Path path) {
        var name = Objects.requireNonNull(path, "path cannot be null").toString();

        for (var c : ESCAPE_CHARACTERS.getBytes(StandardCharsets.UTF_8)) {
            var charString = new String(new byte[] { c }, StandardCharsets.UTF_8);
            var hex = StringUtils.leftPad(Integer.toHexString(c).toUpperCase(), 2, '0');
            name = name.replaceAll(charString, "%" + hex);
        }

        return name;
    }
}
