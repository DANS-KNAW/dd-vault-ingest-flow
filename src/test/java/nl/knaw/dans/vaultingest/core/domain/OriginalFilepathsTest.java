package nl.knaw.dans.vaultingest.core.domain;

import nl.knaw.dans.vaultingest.core.domain.OriginalFilepaths;
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