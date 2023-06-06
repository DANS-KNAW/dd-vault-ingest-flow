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
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.Outbox;
import nl.knaw.dans.vaultingest.core.rdabag.RdaBagWriter;
import nl.knaw.dans.vaultingest.core.rdabag.output.BagOutputWriterFactory;
import nl.knaw.dans.vaultingest.core.validator.BagValidator;
import nl.knaw.dans.vaultingest.core.validator.InvalidBagException;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class DepositToBagProcess {

    private final RdaBagWriter rdaBagWriter;
    private final BagOutputWriterFactory bagOutputWriterFactory;
    private final VaultCatalogService vaultCatalogService;
    private final DepositManager depositManager;
    private final BagValidator bagValidator;
    private final IdMinter idMinter;

    public DepositToBagProcess(
        RdaBagWriter rdaBagWriter,
        BagOutputWriterFactory bagOutputWriterFactory,
        VaultCatalogService vaultCatalogService,
        DepositManager depositManager,
        BagValidator bagValidator,
        IdMinter idMinter) {
        this.rdaBagWriter = rdaBagWriter;
        this.bagOutputWriterFactory = bagOutputWriterFactory;
        this.vaultCatalogService = vaultCatalogService;
        this.depositManager = depositManager;
        this.bagValidator = bagValidator;
        this.idMinter = idMinter;
    }

    public void process(Path path, Outbox outbox) {
        try {
            log.info("Validating deposit on path {}", path);
            bagValidator.validate(path);

            log.info("Loading deposit on path {}", path);
            var deposit = depositManager.loadDeposit(path);
            processDeposit(deposit);

            log.info("Deposit {} processed successfully", deposit.getId());
            depositManager.saveDeposit(deposit);

            log.info("Moving deposit to outbox");
            outbox.moveDeposit(deposit);
        }
        catch (InvalidBagException e) {
            handleFailedDeposit(path, outbox, Deposit.State.REJECTED, e);
        }
        catch (Throwable e) {
            handleFailedDeposit(path, outbox, Deposit.State.FAILED, e);
        }
    }

    void handleFailedDeposit(Path path, Outbox outbox, Deposit.State state, Throwable error) {
        log.error("Deposit on path {} failed with state {}", path, state, error);

        try {
            depositManager.updateDepositState(path, state, error.getMessage());
            outbox.move(path, state);
        }
        catch (IOException e) {
            log.error("Failed to move deposit to outbox", e);
        }
    }

    void processDeposit(Deposit deposit) throws InvalidBagException {

        if (deposit.isUpdate()) {
            // check if deposit exists in vault catalog
            var catalogDeposit = vaultCatalogService.findDeposit(deposit.getSwordToken())
                .orElseThrow(() -> new InvalidBagException(String.format("Deposit with sword token %s not found in vault catalog", deposit.getSwordToken())));

            // compare user id
            if (!StringUtils.equals(deposit.getDepositorId(), catalogDeposit.getDataSupplier())) {
                throw new InvalidBagException(String.format(
                    "Depositor id %s does not match the depositor id %s in the vault catalog", deposit.getDepositorId(), catalogDeposit.getDataSupplier()
                ));
            }

            deposit.setNbn(catalogDeposit.getNbn());
        }
        else {
            // generate nbn for new deposit
            deposit.setNbn(idMinter.mintUrnNbn());
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

}
