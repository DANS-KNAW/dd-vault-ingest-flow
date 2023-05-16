package nl.knaw.dans.vaultingest.core.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class ChecksumManifestEntry {
    private String algorithm;
    private String checksum;
}
