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
import nl.knaw.dans.vaultingest.core.validator.InvalidDepositException;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogService;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

@Slf4j
public class DepositToBagProcess {

    // TODO extract to new class
    private static final String nbnPrefix = "nl:ui:13-";
    private final RdaBagWriter rdaBagWriter;
    private final BagOutputWriterFactory bagOutputWriterFactory;
    private final VaultCatalogService vaultCatalogService;

    public DepositToBagProcess(RdaBagWriter rdaBagWriter, BagOutputWriterFactory bagOutputWriterFactory, VaultCatalogService vaultCatalogService) {
        this.rdaBagWriter = rdaBagWriter;
        this.bagOutputWriterFactory = bagOutputWriterFactory;
        this.vaultCatalogService = vaultCatalogService;
    }

    public void process(Deposit deposit) throws InvalidDepositException {

        // TODO
        // - deposit loading and processing is separated here, but how do we update the deposit if it failed to load?
        // - think about where the state should be set
        // - think about how to handle the deposit state in case of failure
        if (deposit.isUpdate()) {
            // check if deposit exists in vault catalog
            var catalogDeposit = vaultCatalogService.findDeposit(deposit.getSwordToken())
                .orElseThrow(() -> new InvalidDepositException(String.format("Deposit with sword token %s not found in vault catalog", deposit.getSwordToken())));

            // compare user id
            if (!StringUtils.equals(deposit.getDepositorId(), catalogDeposit.getDataSupplier())) {
                throw new InvalidDepositException(String.format(
                    "Depositor id %s does not match the depositor id %s in the vault catalog", deposit.getDepositorId(), catalogDeposit.getDataSupplier()
                ));
            }

            deposit.setNbn(catalogDeposit.getNbn());
        }
        else {
            // generate nbn for new deposit
            deposit.setNbn(mintUrnNbn());
        }

        // send rda bag to vault
        try {
            try (var writer = bagOutputWriterFactory.createBagOutputWriter(deposit)) {
                rdaBagWriter.write(deposit, writer);
            }

            deposit.setState(Deposit.State.ACCEPTED, "Deposit accepted");
        }
        catch (Exception e) {
            // TODO throw some kind of FAILURE state, which is different from REJECTED
            log.error("Error writing bag", e);
            e.printStackTrace();
        }

        vaultCatalogService.registerDeposit(deposit);

    }

    String mintUrnNbn() {
        return String.format("urn:nbn:%s%s", nbnPrefix, UUID.randomUUID());
    }

}
