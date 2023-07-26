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
package nl.knaw.dans.vaultingest.core.rdabag;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManifestConverterTest {

    @Test
    void convert_should_add_relative_paths() {
        var rootDir = Path.of("/a/b/c");
        var paths = Map.of(
            Path.of("/a/b/c/d/e"), "1234",
            Path.of("/a/b/c/d/f"), "5678"
        );
        var result = new ManifestConverter().convert(rootDir, paths);

        assertThat(result).contains("1234  d/e");
        assertThat(result).contains("5678  d/f");
    }
    @Test
    void convert_should_not_accept_paths_not_relative_to_root() {
        var rootDir = Path.of("/a/b/c");
        var paths = Map.of(
            Path.of("/a/b/c/d/e"), "1234",
            Path.of("/a/b/c/d/f"), "5678",
            Path.of("/a/b/x/y/z"), "9012"
        );

        assertThatThrownBy(() -> new ManifestConverter().convert(rootDir, paths))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Path /a/b/x/y/z is not a child of /a/b/c");
    }
    @Test
    void convert_should_relativize_relative_paths() {
        var rootDir = Path.of("/a/b/c");
        var paths = Map.of(
            Path.of("x"), "1234",
            Path.of("y"), "5678"
        );

        var result = new ManifestConverter().convert(rootDir, new TreeMap<>(paths));
        assertThat(result).isEqualTo("1234  x\n5678  y\n");
    }
}