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

import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.datacite.DataciteConverter;
import nl.knaw.dans.vaultingest.core.datacite.DataciteSerializer;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.deposit.DepositFile;
import nl.knaw.dans.vaultingest.core.oaiore.OaiOreConverter;
import nl.knaw.dans.vaultingest.core.oaiore.OaiOreSerializer;
import nl.knaw.dans.vaultingest.core.pidmapping.PidMappingConverter;
import nl.knaw.dans.vaultingest.core.pidmapping.PidMappingSerializer;
import nl.knaw.dans.vaultingest.core.rdabag.output.BagOutputWriter;
import nl.knaw.dans.vaultingest.core.rdabag.output.MultiDigestInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
public class RdaBagWriter {

    private final DataciteSerializer dataciteSerializer;
    private final PidMappingSerializer pidMappingSerializer;
    private final OaiOreSerializer oaiOreSerializer;

    private final DataciteConverter dataciteConverter;
    private final PidMappingConverter pidMappingConverter;
    private final OaiOreConverter oaiOreConverter;

    private final Map<Path, Map<SupportedAlgorithm, String>> checksums;
    private final ManifestConverter manifestConverter;
    private Set<SupportedAlgorithm> requiredAlgorithms;
    private final Path basePath;

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
        this.manifestConverter = new ManifestConverter();

        this.checksums = new HashMap<>();
        this.basePath = Path.of("");
    }

    public void write(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        this.requiredAlgorithms = getAlgorithms(deposit);

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

        log.info("Writing manifest-*.txt files");
        writePayloadManifests(deposit, outputWriter);

        // must be last, because all other files must have been written to
        log.info("Writing tagmanifest-*.txt files");
        writeTagManifest(deposit, outputWriter);
    }

    private void writePayloadFiles(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        for (var file : deposit.getPayloadFiles()) {
            var targetPath = file.getPath();
            var existingChecksums = file.getChecksums();
            var allChecksums = new HashMap<>(existingChecksums);
            var checksumsToCalculate = getAlgorithmsToCalculate(existingChecksums.keySet());
            log.debug("Checksums already present: {}", existingChecksums);
            log.debug("Checksums to calculate: {}", checksumsToCalculate);

            try (var inputStream = file.openInputStream();
                var digestInputStream = new MultiDigestInputStream(inputStream, checksumsToCalculate)) {

                log.info("Writing payload file {} to output", targetPath);
                outputWriter.writeBagItem(digestInputStream, targetPath);

                var newChecksums = digestInputStream.getChecksums();
                log.debug("Newly calculated checksums: {}", newChecksums);

                allChecksums.putAll(digestInputStream.getChecksums());
            }
            catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Algorithm not supported", e);
            }

            checksums.put(targetPath, allChecksums);
        }
    }

    private void writeTagManifest(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        // get the metadata, which is everything EXCEPT the data/** and tagmanifest-* files
        // but the deposit does not know about these files, only this class knows
        var algorithms = getAlgorithms(deposit);

        for (var algorithm : algorithms) {
            var mappings = new TreeMap<Path, String>();
            var payloadPaths = deposit.getPayloadFiles().stream().map(DepositFile::getPath).collect(Collectors.toSet());

            for (var entry : checksums.entrySet()) {
               if (payloadPaths.contains(entry.getKey()) || entry.getKey().startsWith("tagmanifest-")) {
                    continue;
                }

                var path = entry.getKey();
                var checksum = entry.getValue().get(algorithm);

                mappings.put(path, checksum);
            }

            var outputString = manifestConverter.convert(deposit.getBagDir(), mappings);
            var outputFile = String.format("tagmanifest-%s.txt", algorithm.getBagitName());
            outputWriter.writeBagItem(new ByteArrayInputStream(outputString.getBytes()), basePath.resolve(outputFile));
        }

    }

    private void writePayloadManifests(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var checksumMap = new TreeMap<>(
            deposit.getPayloadFiles()
                .stream()
                .map(DepositFile::getPath)
                .map(item -> Map.entry(item, checksums.get(item)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        var algorithms = getAlgorithms(deposit);

        for (var algorithm : algorithms) {
            var outputFile = String.format("manifest-%s.txt", algorithm.getBagitName());
            log.debug("Writing {} ", outputFile);

            var checksumsForAlgorithm = new TreeMap<Path, String>();

            for (var entry : checksumMap.entrySet()) {
                var path = entry.getKey();
                var checksum = entry.getValue().get(algorithm);

                checksumsForAlgorithm.put(path, checksum);
            }

            var content = manifestConverter.convert(deposit.getBagDir(), checksumsForAlgorithm);

            log.trace("Contents for {}: \n{}", outputFile, content);
            checksummedWriteToOutput(content, basePath.resolve(outputFile), outputWriter);
        }
    }

    private void writeDatacite(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var resource = dataciteConverter.convert(deposit);
        var dataciteXml = dataciteSerializer.serialize(resource);

        checksummedWriteToOutput(dataciteXml, basePath.resolve("metadata/datacite.xml"), outputWriter);
    }

    private void writeOaiOre(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var oaiOre = oaiOreConverter.convert(deposit);

        var rdf = oaiOreSerializer.serializeAsRdf(oaiOre);
        var jsonld = oaiOreSerializer.serializeAsJsonLd(oaiOre);

        checksummedWriteToOutput(rdf, basePath.resolve("metadata/oai-ore.rdf"), outputWriter);
        checksummedWriteToOutput(jsonld, basePath.resolve("metadata/oai-ore.jsonld"), outputWriter);
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
            basePath.resolve("metadata/pid-mapping.txt"),
            outputWriter
        );
    }

    private void writeBagitFile(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var bagitPath = basePath.resolve("bagit.txt");

        try (var input = deposit.inputStreamForMetadataFile(bagitPath)) {
            checksummedWriteToOutput(input, bagitPath, outputWriter);
        }
    }

    private void writeBagInfo(Deposit deposit, BagOutputWriter outputWriter) throws IOException {
        var baginfoPath = basePath.resolve("bag-info.txt");

        try (var input = deposit.inputStreamForMetadataFile(baginfoPath)) {
            checksummedWriteToOutput(input, baginfoPath, outputWriter);
        }
    }

    private void checksummedWriteToOutput(InputStream inputStream, Path path, BagOutputWriter outputWriter) throws IOException {
        try (var input = new MultiDigestInputStream(inputStream, requiredAlgorithms)) {
            outputWriter.writeBagItem(input, path);
            var result = input.getChecksums();
            log.trace("Checksums for {}: {}", path, result);
            checksums.put(path, result);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithm not supported", e);
        }
    }

    private void checksummedWriteToOutput(String string, Path path, BagOutputWriter outputWriter) throws IOException {
        checksummedWriteToOutput(new ByteArrayInputStream(string.getBytes()), path, outputWriter);
    }

    Set<SupportedAlgorithm> getAlgorithms(Deposit deposit) {
        var algorithms = new HashSet<>(deposit.getPayloadManifestAlgorithms());

        // if there is only 1 algorithm, and it is MD5, then we also need SHA1
        if (algorithms.size() <= 1 && algorithms.contains(StandardSupportedAlgorithms.MD5)) {
            algorithms.add(StandardSupportedAlgorithms.SHA1);
        }

        log.trace("Algorithms for deposit {}: {}", deposit.getId(), algorithms);
        return algorithms;
    }

    Set<SupportedAlgorithm> getAlgorithmsToCalculate(Set<SupportedAlgorithm> existingChecksums) {
        if (existingChecksums.size() <= 1 && existingChecksums.contains(StandardSupportedAlgorithms.MD5)) {
            return Set.of(StandardSupportedAlgorithms.SHA1);
        }

        return Set.of();
    }
}
