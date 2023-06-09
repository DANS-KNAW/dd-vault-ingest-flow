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
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.ManifestAlgorithm;
import nl.knaw.dans.vaultingest.core.rdabag.converter.DataciteConverter;
import nl.knaw.dans.vaultingest.core.rdabag.converter.OaiOreConverter;
import nl.knaw.dans.vaultingest.core.rdabag.converter.PidMappingConverter;
import nl.knaw.dans.vaultingest.core.rdabag.output.BagOutputWriter;
import nl.knaw.dans.vaultingest.core.rdabag.output.MultiDigestInputStream;
import nl.knaw.dans.vaultingest.core.rdabag.serializer.DataciteSerializer;
import nl.knaw.dans.vaultingest.core.rdabag.serializer.OaiOreSerializer;
import nl.knaw.dans.vaultingest.core.rdabag.serializer.PidMappingSerializer;
import org.apache.commons.io.output.NullOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RdaBagWriter {

    private final DataciteSerializer dataciteSerializer;
    private final PidMappingSerializer pidMappingSerializer;
    private final OaiOreSerializer oaiOreSerializer;

    private final DataciteConverter dataciteConverter;
    private final PidMappingConverter pidMappingConverter;
    private final OaiOreConverter oaiOreConverter;

    private final Map<Path, Map<ManifestAlgorithm, String>> checksums;
    private final List<ManifestAlgorithm> requiredAlgorithms = List.of(ManifestAlgorithm.SHA1, ManifestAlgorithm.MD5);

    RdaBagWriter(
        DataciteSerializer dataciteSerializer,
        PidMappingSerializer pidMappingSerializer,
        OaiOreSerializer oaiOreSerializer,
        DataciteConverter dataciteConverter,
        PidMappingConverter pidMappingConverter,
        OaiOreConverter oaiOreConverter
    ) {
        this.dataciteSerializer = dataciteSerializer;
        this.pidMappingSerializer = pidMappingSerializer;
        this.oaiOreSerializer = oaiOreSerializer;
        this.dataciteConverter = dataciteConverter;
        this.pidMappingConverter = pidMappingConverter;
        this.oaiOreConverter = oaiOreConverter;

        this.checksums = new HashMap<>();
    }

    public void write(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        log.info("Writing payload files");
        writePayloadFiles(deposit, outputWriter);

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

        writeManifests(deposit, outputWriter);

        // must be last, because all other files must have been written to
        writeTagManifest(deposit, outputWriter);
    }

    private void writePayloadFiles(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        for (var file : deposit.getPayloadFiles()) {
            var targetPath = file.getPath();
            var existingChecksums = file.getChecksums();
            var checksumsToCalculate = requiredAlgorithms.stream()
                .filter(algorithm -> !existingChecksums.containsKey(algorithm))
                .collect(Collectors.toList());

            var allChecksums = new HashMap<>(existingChecksums);
            log.debug("Checksums already present: {}", existingChecksums);

            try (var inputStream = file.openInputStream();
                 var digestInputStream = new MultiDigestInputStream(inputStream, checksumsToCalculate)) {

                log.info("Writing payload file {} to output", targetPath);
                outputWriter.writeBagItem(digestInputStream, targetPath);

                var newChecksums = digestInputStream.getChecksums();
                log.debug("Newly calculated checksums: {}", newChecksums);

                allChecksums.putAll(digestInputStream.getChecksums());
            }

            checksums.put(targetPath, allChecksums);
        }
    }

    private void writeTagManifest(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        // get the metadata, which is everything EXCEPT the data/** and tagmanifest-* files
        // but the deposit does not know about these files, only this class knows
        for (var algorithm : requiredAlgorithms) {
            var outputString = new StringBuilder();
            var payloadPaths = deposit.getPayloadFiles().stream().map(DepositFile::getPath).collect(Collectors.toSet());

            for (var entry : checksums.entrySet()) {
                if (payloadPaths.contains(entry.getKey()) || entry.getKey().startsWith("tagmanifest-")) {
                    continue;
                }

                var path = entry.getKey();
                var checksum = entry.getValue().get(algorithm);

                outputString.append(String.format("%s  %s\n", checksum, path));
            }

            var outputFile = String.format("tagmanifest-%s.txt", algorithm.getName());
            outputWriter.writeBagItem(new ByteArrayInputStream(outputString.toString().getBytes()), Path.of(outputFile));
        }

    }

    private void writeManifests(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        // iterate all files in rda bag and get checksum sha1
        var checksumMap = new HashMap<DepositFile, Map<ManifestAlgorithm, String>>();

        for (var file : deposit.getPayloadFiles()) {
            var output = (OutputStream) NullOutputStream.NULL_OUTPUT_STREAM;

            try (var input = new MultiDigestInputStream(file.openInputStream(), requiredAlgorithms)) {
                input.transferTo(output);
                checksumMap.put(file, input.getChecksums());
            }
        }

        for (var algorithm : requiredAlgorithms) {
            var outputFile = String.format("manifest-%s.txt", algorithm.getName());
            var outputString = new StringBuilder();

            for (var file : deposit.getPayloadFiles()) {
                var checksum = checksumMap.get(file).get(algorithm);
                outputString.append(String.format("%s  %s\n", checksum, file.getPath()));
            }

            checksummedWriteToOutput(outputString.toString(), Path.of(outputFile), outputWriter);
        }
    }

    private void writeDatacite(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var resource = dataciteConverter.convert(deposit);
        var dataciteXml = dataciteSerializer.serialize(resource);

        checksummedWriteToOutput(dataciteXml, Path.of("metadata/datacite.xml"), outputWriter);
    }

    private void writeOaiOre(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var oaiOre = oaiOreConverter.convert(deposit);

        var rdf = oaiOreSerializer.serializeAsRdf(oaiOre);
        var jsonld = oaiOreSerializer.serializeAsJsonLd(oaiOre);

        checksummedWriteToOutput(rdf, Path.of("metadata/oai-ore.rdf"), outputWriter);
        checksummedWriteToOutput(jsonld, Path.of("metadata/oai-ore.jsonld"), outputWriter);
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
            pidMappingsSerialized,
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
        try (var input = new MultiDigestInputStream(inputStream, requiredAlgorithms)) {
            outputWriter.writeBagItem(input, path);
            checksums.put(path, input.getChecksums());
        }
    }

    void checksummedWriteToOutput(String string, Path path, BagOutputWriter outputWriter) throws IOException {
        checksummedWriteToOutput(new ByteArrayInputStream(string.getBytes()), path, outputWriter);
    }
}
