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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilepathConverterTest {

    @Test
    void convertFilepath_should_not_change_valid_characters() {
        assertThat(FilepathConverter.convertFilepath(Path.of("/a/b/c^!@#$&*()")))
            .isEqualTo("/a/b/c^!@#$&*()");
    }

    @Test
    void convertFilepath_should_encode_special_characters() {
        assertThat(FilepathConverter.convertFilepath(Path.of("/a/name\nwith\r\tnewlines%and%percent")))
            .isEqualTo("/a/name%0Awith%0D\tnewlines%25and%25percent");
    }

    @Test
    void convertFilepath_should_not_accept_null_paths() {
        assertThatThrownBy(() -> FilepathConverter.convertFilepath(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("path cannot be null");
    }
}