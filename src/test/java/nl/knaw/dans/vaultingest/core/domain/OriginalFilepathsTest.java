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

class OriginalFilepathsTest {

    OriginalFilepaths buildOriginalFilepaths() {
        var result = new OriginalFilepaths();
        result.addMapping(Path.of("data/in/a/nice/way"), Path.of("data/123456789"));
        result.addMapping(Path.of("data/in/another path/ with spaces"), Path.of("data/abc-def"));

        return result;
    }

    @Test
    void getLogicalPath_should_return_mapping() {
        var paths = buildOriginalFilepaths();

        // the file on disk would be called data/123456789
        var pathOnDisk = Path.of("data/123456789");
        var pathInBag = Path.of("data/in/a/nice/way");

        assertEquals(pathInBag, paths.getLogicalPath(pathOnDisk));
    }

    @Test
    void getLogicalPath_should_return_same_value_for_nonexisting_mapping() {
        var paths = buildOriginalFilepaths();

        // the file on disk would be called data/in/a/nice/way
        var pathInBag = Path.of("data/in/a/nice/way");
        assertEquals(pathInBag, paths.getLogicalPath(pathInBag));
    }

    @Test
    void getLogicalPath_should_work_with_spaces() {
        var paths = buildOriginalFilepaths();

        // the file on disk would be called data/in/a/nice/way
        var pathInBag = Path.of("data/in/another path/ with spaces");
        var pathOnDisk = Path.of("data/abc-def");

        assertEquals(pathInBag, paths.getLogicalPath(pathOnDisk));
    }

    @Test
    void getPhysicalPath() {
        var paths = buildOriginalFilepaths();

        // the file on disk would be called data/123456789
        var pathOnDisk = Path.of("data/123456789");
        var pathInBag = Path.of("data/in/a/nice/way");

        assertEquals(pathOnDisk, paths.getPhysicalPath(pathInBag));
    }

    @Test
    void getPhysicalPath_should_return_same_value_for_nonexisting_mapping() {
        var paths = buildOriginalFilepaths();

        // the file on disk would be called data/in/a/nice/way
        var pathOnDisk = Path.of("data/no/mapping/here");
        assertEquals(pathOnDisk, paths.getPhysicalPath(pathOnDisk));
    }
}