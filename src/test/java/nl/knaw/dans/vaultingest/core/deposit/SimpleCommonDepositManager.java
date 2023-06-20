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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.utilities.TestLanguageResolver;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class SimpleCommonDepositManager extends CommonDepositManager {


    public SimpleCommonDepositManager(CountryResolver countryResolver) throws IOException {
        super(
            new XmlReaderImpl(),
            new TestLanguageResolver(),
            countryResolver
        );
    }

    @Override
    public Deposit loadDeposit(Path inputPath) {
        try {
            var resource = getClass().getResource(inputPath.toString());
            assert resource != null;

            var path = Path.of(resource.getPath());

            return super.loadDeposit(path);
        }
        catch (Exception e) {
            throw new RuntimeException("Error loading deposit", e);
        }
    }

    @Override
    public void saveDeposit(Deposit deposit) {
        log.info("Ignoring saveDeposit");
    }

    @Override
    public void updateDepositState(Path path, Deposit.State state, String message) {
        log.info("Ignoring updateDepositState");
    }
}
