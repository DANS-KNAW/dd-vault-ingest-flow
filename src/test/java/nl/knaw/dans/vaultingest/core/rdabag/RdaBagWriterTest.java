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
import nl.knaw.dans.vaultingest.core.datacite.DataciteConverter;
import nl.knaw.dans.vaultingest.core.datacite.DataciteSerializer;
import nl.knaw.dans.vaultingest.core.oaiore.OaiOreConverter;
import nl.knaw.dans.vaultingest.core.oaiore.OaiOreSerializer;
import nl.knaw.dans.vaultingest.core.pidmapping.PidMappingConverter;
import nl.knaw.dans.vaultingest.core.pidmapping.PidMappingSerializer;
import nl.knaw.dans.vaultingest.core.utilities.CountryResolverFactory;
import nl.knaw.dans.vaultingest.core.utilities.InMemoryOutputWriter;
import nl.knaw.dans.vaultingest.core.utilities.LanguageResolverFactory;
import nl.knaw.dans.vaultingest.core.utilities.TestDepositManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RdaBagWriterTest {

    @Test
    void write_should_handle_md5_only_bags() throws Exception {
        var writer = new RdaBagWriter(
            new DataciteSerializer(),
            new PidMappingSerializer(),
            new OaiOreSerializer(new ObjectMapper()),
            new DataciteConverter(),
            new PidMappingConverter(),
            new OaiOreConverter(LanguageResolverFactory.getInstance(), CountryResolverFactory.getInstance())
        );

        var manager = new TestDepositManager();
        var deposit = manager.loadDeposit(Path.of("/input/bag-md5/c169676f-5315-4d86-bde0-a62dbc915228"));
        var output = new InMemoryOutputWriter();
        writer.write(deposit, output);

        assertThat(output.getData().keySet()).contains(
            Path.of("manifest-md5.txt"),
            Path.of("tagmanifest-md5.txt"),
            Path.of("manifest-sha1.txt"),
            Path.of("tagmanifest-sha1.txt")
        );
    }

    @Test
    void write_should_not_write_md5_if_sha1_is_present() throws Exception {
        var writer = new RdaBagWriter(
            new DataciteSerializer(),
            new PidMappingSerializer(),
            new OaiOreSerializer(new ObjectMapper()),
            new DataciteConverter(),
            new PidMappingConverter(),
            new OaiOreConverter(LanguageResolverFactory.getInstance(), CountryResolverFactory.getInstance())
        );

        var manager = new TestDepositManager();
        var deposit = manager.loadDeposit(Path.of("/input/bag-sha1/c169676f-5315-4d86-bde0-a62dbc915228"));
        var output = new InMemoryOutputWriter();
        writer.write(deposit, output);

        assertThat(output.getData().keySet()).contains(
            Path.of("manifest-sha1.txt"),
            Path.of("tagmanifest-sha1.txt"),
            Path.of("manifest-sha256.txt"),
            Path.of("tagmanifest-sha256.txt")
        );

        assertThat(output.getData().keySet()).doesNotContain(
            Path.of("manifest-md5.txt"),
            Path.of("tagmanifest-md5.txt")
        );
    }
}