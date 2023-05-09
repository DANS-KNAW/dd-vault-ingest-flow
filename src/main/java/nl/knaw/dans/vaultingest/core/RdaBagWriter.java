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
package nl.knaw.dans.vaultingest.core;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.domain.DepositBag;
import nl.knaw.dans.vaultingest.core.domain.RdaBag;
import nl.knaw.dans.vaultingest.core.serializer.DataciteSerializer;
import nl.knaw.dans.vaultingest.core.serializer.OaiOreSerializer;
import nl.knaw.dans.vaultingest.core.serializer.PidMappingSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class RdaBagWriter {

    private final DataciteSerializer dataciteSerializer = new DataciteSerializer();
    private final PidMappingSerializer pidMappingSerializer = new PidMappingSerializer();

    private final OaiOreSerializer oaiOreSerializer = new OaiOreSerializer();

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

        // TODO write manifest file
        // can be copied from the original
        // just need to make sure the right version exists (sha1)
        // TODO write tagmanifest file
        // this should be recalculated and output with the right algorithm
    }

    private void writeDatacite(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var dataciteXml = dataciteSerializer.serialize(rdaBag.getResource());
        outputWriter.writeBagItem(new ByteArrayInputStream(dataciteXml.getBytes()), Path.of("metadata/datacite.xml"));
    }

    private void writeOaiOre(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var oaiOre = rdaBag.getOreResourceMap();

        outputWriter.writeBagItem(new ByteArrayInputStream(oaiOre.toRDF().getBytes()), Path.of("metadata/oai-ore.rdf"));
        outputWriter.writeBagItem(new ByteArrayInputStream(oaiOre.toJsonLD().getBytes()), Path.of("metadata/oai-ore.jsonld"));
    }

    private void writeMetadataFile(DepositBag bag, Path metadataFile, BagOutputWriter outputWriter) throws IOException {
        try (var inputStream = bag.inputStreamForMetadataFile(metadataFile)) {
            outputWriter.writeBagItem(inputStream, metadataFile);
        }
    }

    private void writePidMappings(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var output = new ByteArrayOutputStream();
        pidMappingSerializer.serialize(rdaBag.getPidMappings(), output);

        outputWriter.writeBagItem(
                new ByteArrayInputStream(output.toByteArray()),
                Path.of("metadata/pid-mapping.txt")
        );
    }

    private void writeBagitFile(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var bag = rdaBag.getBag();
        var bagitPath = Path.of("bagit.txt");

        try (var input = bag.getBagItFile()) {
            outputWriter.writeBagItem(input, bagitPath);
        }
    }

    private void writeBagInfo(RdaBag rdaBag, BagOutputWriter outputWriter) throws IOException {
        var bag = rdaBag.getBag();
        var bagitPath = Path.of("bag-info.txt");

        try (var input = bag.getBagInfoFile()) {
            outputWriter.writeBagItem(input, bagitPath);
        }
    }
}
