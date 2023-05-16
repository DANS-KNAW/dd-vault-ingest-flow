package nl.knaw.dans.vaultingest.core.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;
import java.util.Map;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class ChecksumManifest {
    private Path path;
    private Map<String, ChecksumManifestEntry> entries;
}
