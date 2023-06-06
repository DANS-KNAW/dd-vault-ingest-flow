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
package nl.knaw.dans.vaultingest.core.deposit;

import nl.knaw.dans.vaultingest.core.utilities.EchoDatasetContactResolver;
import nl.knaw.dans.vaultingest.core.utilities.TestLanguageResolver;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CommonDepositManagerIntegrationTest {

    @Test
    void loadDeposit() throws Exception {
        var factory = new CommonDepositManager(
            new XmlReaderImpl(),
            new EchoDatasetContactResolver(),
            new TestLanguageResolver()
        );

        var s = getClass().getResource("/input/0b9bb5ee-3187-4387-bb39-2c09536c79f7");
        assert s != null;

        var deposit = factory.loadDeposit(Path.of(s.getPath()));
        assertEquals("0b9bb5ee-3187-4387-bb39-2c09536c79f7", deposit.getId());
    }
}