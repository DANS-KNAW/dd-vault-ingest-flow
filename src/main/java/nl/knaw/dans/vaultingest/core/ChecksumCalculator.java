package nl.knaw.dans.vaultingest.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.NullOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ChecksumCalculator {

    public Map<String, String> calculateChecksums(InputStream inputStream, Collection<String> algorithms) {
        var result = new HashMap<String, String>();

        var output = (OutputStream) NullOutputStream.NULL_OUTPUT_STREAM;
        var streams = new HashMap<String, DigestOutputStream>();

        for (var alg : algorithms) {
            try {
                var digest = MessageDigest.getInstance(alg);
                output = new DigestOutputStream(output, digest);
                streams.put(alg, (DigestOutputStream) output);
            } catch (NoSuchAlgorithmException e) {
                log.error("Algorithm {} not supported", alg, e);
            }
        }

        try {
            inputStream.transferTo(output);
        } catch (IOException e) {
            log.error("Error calculating checksums", e);
        }

        for (var entry : streams.entrySet()) {
            result.put(entry.getKey(), bytesToHex(entry.getValue().getMessageDigest().digest()));
        }

        return result;
    }

    private String bytesToHex(byte[] digest) {
        var sb = new StringBuilder();
        for (var b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
