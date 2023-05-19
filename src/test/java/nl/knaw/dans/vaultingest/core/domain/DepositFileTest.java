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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepositFileTest {

    @Test
    void getDirectoryLabel_should_return_same_path_for_valid_characters() {
        var depositFile = DepositFile.builder()
            .filepath("data/only/valid/characters.txt")
            .build();

        assertEquals(Path.of("only/valid/"), depositFile.getDirectoryLabel());
    }

    @Test
    void getDirectoryLabel_should_return_underscores_for_invalid_characters() {
        var depositFile = DepositFile.builder()
            .filepath("data/&invalid**/(characters)))/characters.txt")
            .build();

        assertEquals(Path.of("_invalid__/_characters___/"), depositFile.getDirectoryLabel());
    }

    @Test
    void getFilename_should_return_same_value_for_valid_characters() {
        var depositFile = DepositFile.builder()
            .filepath("data/valid/characters.txt")
            .build();

        assertEquals(Path.of("characters.txt"), depositFile.getFilename());
    }

    @Test
    void getFilename_should_return_underscores_for_invalid_characters() {
        var depositFile = DepositFile.builder()
            .filepath("data/invalid/characters/here:*?\"<>|;#.txt")
            .build();

        assertEquals(Path.of("here_________.txt"), depositFile.getFilename());
    }

    @Test
    void getPath_should_not_format_output() {
        var depositFile = DepositFile.builder()
            .filepath("data/invalid/characters/here:*?\"<>|;#.txt")
            .build();

        assertEquals(Path.of("data/invalid/characters/here:*?\"<>|;#.txt"), depositFile.getPath());
    }

    @Test
    void getRelativePath_should_transform_output() {
        var depositFile = DepositFile.builder()
            .filepath("data/invalid/characters/here:*?\"<>|;#.txt")
            .build();

        assertEquals(Path.of("invalid/characters/here_________.txt"), depositFile.getRelativePath());
    }
}