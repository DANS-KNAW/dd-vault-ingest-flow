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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.ChecksumCalculator;
import nl.knaw.dans.vaultingest.core.domain.DepositBag;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.RdaBag;
import nl.knaw.dans.vaultingest.core.rdabag.output.BagOutputWriter;
import nl.knaw.dans.vaultingest.core.serializer.DataciteSerializer;
import nl.knaw.dans.vaultingest.core.serializer.PidMappingSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class RdaBagWriter {

    private final DataciteSerializer dataciteSerializer = new DataciteSerializer();
    private final PidMappingSerializer pidMappingSerializer = new PidMappingSerializer();

    private Map<Path, Map<String, String>> checksums = new HashMap<>();

    public void write(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var bag = rdaBag.getBag();

        for (var file : bag.getPayloadFiles()) {
            log.info("Writing payload file {}", file);
            outputWriter.writeBagItem(bag.inputStreamForPayloadFile(file), file.getPath());
        }

        log.info("Writing metadata/datacite.xml");
        writeDatacite(rdaBag, outputWriter);

        log.info("Writing metadata/oai-ore");
        writeOaiOre(rdaBag, outputWriter);

        log.info("Writing metadata/pid-mapping.txt");
        writePidMappings(rdaBag, outputWriter);

        log.info("Writing bag-info.txt");
        writeBagInfo(rdaBag, outputWriter);

        log.info("Writing bagit.txt");
        writeBagitFile(rdaBag, outputWriter);

        for (var metadataFile : bag.getMetadataFiles()) {
            log.info("Writing {}", metadataFile);
            writeMetadataFile(bag, metadataFile, outputWriter);
        }

        writeManifests(rdaBag, outputWriter);

        // must be last, because all other files must have been written to
        writeTagManifest(rdaBag, outputWriter);

        outputWriter.close();
    }

    private void writeTagManifest(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var bag = rdaBag.getBag();
        var files = bag.getMetadataFiles();

        // get the metadata, which is everything EXCEPT the data/** and tagmanifest-* files
        // but the deposit or the rdabag does not know about these files, only this class knows

        var algorithms = List.of("MD5", "SHA-1", "SHA-256");

        for (var algorithm : algorithms) {
            var outputString = new StringBuilder();

            for (var entry : checksums.entrySet()) {
                var path = entry.getKey();
                var checksum = entry.getValue().get(algorithm);

                outputString.append(String.format("%s  %s\n", checksum, path));
            }

            var outputFile = String.format("tagmanifest-%s.txt", algorithm.replaceAll("-", "").toLowerCase());
            outputWriter.writeBagItem(new ByteArrayInputStream(outputString.toString().getBytes()), Path.of(outputFile));
        }
    }

    private void writeManifests(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        // iterate all files in rda bag and get checksum sha1
        var bag = rdaBag.getBag();
        var files = bag.getPayloadFiles();
        var algorithms = List.of("MD5", "SHA-1", "SHA-256");

        var calculator = new ChecksumCalculator();
        var checksumMap = new HashMap<DepositFile, Map<String, String>>();

        for (var file : files) {
            try (var inputStream = bag.inputStreamForPayloadFile(file)) {
                var checksums = calculator.calculateChecksums(inputStream, algorithms);
                checksumMap.put(file, checksums);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (var algorithm : algorithms) {
            var outputFile = String.format("manifest-%s.txt", algorithm.replaceAll("-", "").toLowerCase());
            var outputString = new StringBuilder();

            for (var file : files) {
                var checksum = checksumMap.get(file).get(algorithm);
                outputString.append(String.format("%s  %s\n", checksum, file.getPath()));
            }

            checksummedWriteToOutput(new ByteArrayInputStream(outputString.toString().getBytes()), Path.of(outputFile), outputWriter);
        }
    }

    private void writeDatacite(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var dataciteXml = dataciteSerializer.serialize(rdaBag.getResource());
        checksummedWriteToOutput(new ByteArrayInputStream(dataciteXml.getBytes()), Path.of("metadata/datacite.xml"), outputWriter);
    }

    private void writeOaiOre(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var oaiOre = rdaBag.getOreResourceMap();

        checksummedWriteToOutput(new ByteArrayInputStream(oaiOre.toRDF().getBytes()), Path.of("metadata/oai-ore.rdf"), outputWriter);
        checksummedWriteToOutput(new ByteArrayInputStream(oaiOre.toJsonLD().getBytes()), Path.of("metadata/oai-ore.jsonld"), outputWriter);
    }

    private void writeMetadataFile(DepositBag bag, Path metadataFile, BagOutputWriter outputWriter) throws IOException {
        try (var inputStream = bag.inputStreamForMetadataFile(metadataFile)) {
            checksummedWriteToOutput(inputStream, metadataFile, outputWriter);
        }
    }

    private void writePidMappings(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var pidMappings = pidMappingSerializer.serialize(rdaBag.getPidMappings());

        checksummedWriteToOutput(
            new ByteArrayInputStream(pidMappings.getBytes()),
            Path.of("metadata/pid-mapping.txt"),
            outputWriter
        );
    }

    private void writeBagitFile(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var bag = rdaBag.getBag();
        var bagitPath = Path.of("bagit.txt");

        try (var input = bag.getBagItFile()) {
            checksummedWriteToOutput(input, bagitPath, outputWriter);
        }
    }

    private void writeBagInfo(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var bag = rdaBag.getBag();
        var bagitPath = Path.of("bag-info.txt");

        try (var input = bag.getBagInfoFile()) {
            checksummedWriteToOutput(input, bagitPath, outputWriter);
        }
    }

    void checksummedWriteToOutput(InputStream inputStream, Path path, BagOutputWriter outputWriter) throws IOException {
        var checksumInputStreams = new HashMap<String, DigestInputStream>();

        var input = inputStream;

        for (var alg : List.of("MD5", "SHA-1", "SHA-256")) {
            try {
                input = new DigestInputStream(input, MessageDigest.getInstance(alg));
                checksumInputStreams.put(alg, (DigestInputStream) input);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        outputWriter.writeBagItem(input, path);

        var pathEntry = new HashMap<String, String>();

        for (var entry : checksumInputStreams.entrySet()) {
            pathEntry.put(entry.getKey(), bytesToHex(entry.getValue().getMessageDigest().digest()));
        }

        checksums.put(path, pathEntry);
    }


    private String bytesToHex(byte[] digest) {
        var sb = new StringBuilder();
        for (var b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
