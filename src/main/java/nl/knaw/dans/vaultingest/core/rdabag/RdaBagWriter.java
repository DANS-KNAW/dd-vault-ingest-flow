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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.ChecksumCalculator;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.rdabag.converter.DataciteConverter;
import nl.knaw.dans.vaultingest.core.rdabag.converter.OaiOreConverter;
import nl.knaw.dans.vaultingest.core.rdabag.converter.PidMappingConverter;
import nl.knaw.dans.vaultingest.core.rdabag.output.BagOutputWriter;
import nl.knaw.dans.vaultingest.core.rdabag.serializer.DataciteSerializer;
import nl.knaw.dans.vaultingest.core.rdabag.serializer.OaiOreSerializer;
import nl.knaw.dans.vaultingest.core.rdabag.serializer.PidMappingSerializer;

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

// TODO clean up messy checksum calculations
@Slf4j
public class RdaBagWriter {

    private final DataciteSerializer dataciteSerializer = new DataciteSerializer();
    private final PidMappingSerializer pidMappingSerializer = new PidMappingSerializer();
    private final OaiOreSerializer oaiOreSerializer = new OaiOreSerializer(new ObjectMapper());

    private final DataciteConverter dataciteConverter = new DataciteConverter();
    private final PidMappingConverter pidMappingConverter = new PidMappingConverter();
    private final OaiOreConverter oaiOreConverter = new OaiOreConverter();

    private final Map<Path, Map<String, String>> checksums = new HashMap<>();

    public void write(Deposit deposit, BagOutputWriter outputWriter) throws IOException {

        var dataPath = Path.of("data");

        for (var file : deposit.getPayloadFiles()) {
            log.info("Writing payload file {}", file);
            outputWriter.writeBagItem(file.openInputStream(), dataPath.resolve(file.getPath()));
        }

        log.info("Writing metadata/datacite.xml");
        writeDatacite(deposit, outputWriter);

        log.info("Writing metadata/oai-ore");
        writeOaiOre(deposit, outputWriter);

        log.info("Writing metadata/pid-mapping.txt");
        writePidMappings(deposit, outputWriter);

        log.info("Writing bag-info.txt");
        writeBagInfo(deposit, outputWriter);

        log.info("Writing bagit.txt");
        writeBagitFile(deposit, outputWriter);

        for (var metadataFile : deposit.getMetadataFiles()) {
            log.info("Writing {}", metadataFile);
            writeMetadataFile(deposit, metadataFile, outputWriter);
        }

        writeManifests(deposit, dataPath, outputWriter);

        // must be last, because all other files must have been written to
        writeTagManifest(deposit, outputWriter);
    }

    private void writeTagManifest(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
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

    private void writeManifests(Deposit deposit, Path dataPath, BagOutputWriter outputWriter) throws IOException {
        // iterate all files in rda bag and get checksum sha1
        var files = deposit.getPayloadFiles();
        var algorithms = List.of("MD5", "SHA-1", "SHA-256");

        var calculator = new ChecksumCalculator();
        var checksumMap = new HashMap<DepositFile, Map<String, String>>();

        for (var file : files) {
            try (var inputStream = file.openInputStream()) {
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
                outputString.append(String.format("%s  %s\n", checksum, dataPath.resolve(file.getPath())));
            }

            checksummedWriteToOutput(new ByteArrayInputStream(outputString.toString().getBytes()), Path.of(outputFile), outputWriter);
        }
    }

    private void writeDatacite(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var resource = dataciteConverter.convert(deposit);
        var dataciteXml = dataciteSerializer.serialize(resource);

        checksummedWriteToOutput(new ByteArrayInputStream(dataciteXml.getBytes()), Path.of("metadata/datacite.xml"), outputWriter);
    }

    private void writeOaiOre(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var oaiOre = oaiOreConverter.convert(deposit);

        var rdf = oaiOreSerializer.serializeAsRdf(oaiOre);
        var jsonld = oaiOreSerializer.serializeAsJsonLd(oaiOre);

        checksummedWriteToOutput(new ByteArrayInputStream(rdf.getBytes()), Path.of("metadata/oai-ore.rdf"), outputWriter);
        checksummedWriteToOutput(new ByteArrayInputStream(jsonld.getBytes()), Path.of("metadata/oai-ore.jsonld"), outputWriter);
    }

    private void writeMetadataFile(Deposit deposit, Path metadataFile, BagOutputWriter outputWriter) throws IOException {
        try (var inputStream = deposit.inputStreamForMetadataFile(metadataFile)) {
            checksummedWriteToOutput(inputStream, metadataFile, outputWriter);
        }
    }

    private void writePidMappings(Deposit deposit, BagOutputWriter outputWriter) throws IOException {

        var pidMappings = pidMappingConverter.convert(deposit);
        var pidMappingsSerialized = pidMappingSerializer.serialize(pidMappings);

        checksummedWriteToOutput(
            new ByteArrayInputStream(pidMappingsSerialized.getBytes()),
            Path.of("metadata/pid-mapping.txt"),
            outputWriter
        );
    }

    private void writeBagitFile(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var bagitPath = Path.of("bagit.txt");

        try (var input = deposit.inputStreamForMetadataFile(bagitPath)) {
            checksummedWriteToOutput(input, bagitPath, outputWriter);
        }
    }

    private void writeBagInfo(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var baginfoPath = Path.of("bag-info.txt");

        try (var input = deposit.inputStreamForMetadataFile(baginfoPath)) {
            checksummedWriteToOutput(input, baginfoPath, outputWriter);
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
