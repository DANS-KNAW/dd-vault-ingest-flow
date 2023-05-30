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
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.rdabag.RdaBagWriter;
import nl.knaw.dans.vaultingest.core.rdabag.output.BagOutputWriterFactory;
import nl.knaw.dans.vaultingest.core.validator.DepositValidator;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogService;

@Slf4j
public class DepositToBagProcess {

    private final DepositValidator depositValidator;
    private final RdaBagWriter rdaBagWriter;
    private final BagOutputWriterFactory bagOutputWriterFactory;
    private final VaultCatalogService vaultCatalogService;

    public DepositToBagProcess(DepositValidator depositValidator, RdaBagWriter rdaBagWriter, BagOutputWriterFactory bagOutputWriterFactory, VaultCatalogService vaultCatalogService) {
        this.depositValidator = depositValidator;
        this.rdaBagWriter = rdaBagWriter;
        this.bagOutputWriterFactory = bagOutputWriterFactory;
        this.vaultCatalogService = vaultCatalogService;
    }

    public void process(Deposit deposit) {
        // validate deposit?
        depositValidator.validate(deposit);

        // TODO register deposit with vault catalog
        vaultCatalogService.registerDeposit(deposit);

        // send rda bag to vault
        try {
            try (var writer = bagOutputWriterFactory.createBagOutputWriter(deposit)) {
                rdaBagWriter.write(deposit, writer);
            }
        } catch (Exception e) {
            log.error("Error writing bag", e);
            e.printStackTrace();
        }

        // TODO update deposit with vault catalog
        // TODO move deposit to somewhere
    }
}
