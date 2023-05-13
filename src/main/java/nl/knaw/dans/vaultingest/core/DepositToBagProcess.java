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

import nl.knaw.dans.vaultingest.core.rdabag.DepositRdaBagConverter;
import nl.knaw.dans.vaultingest.core.domain.Deposit;

public class DepositToBagProcess {

    private final DepositRdaBagConverter depositRdaBagConverter;
    private final DepositValidator depositValidator;
    private final RdaBagWriter rdaBagWriter;

    public DepositToBagProcess(DepositRdaBagConverter depositRdaBagConverter, DepositValidator depositValidator, RdaBagWriter rdaBagWriter) {
        this.depositRdaBagConverter = depositRdaBagConverter;
        this.depositValidator = depositValidator;
        this.rdaBagWriter = rdaBagWriter;
    }

    void process(Deposit deposit) {
        // validate deposit?
        // validate bag inside deposit?
        depositValidator.validate(deposit);

        // convert the deposit to a rda bag
        var rdaBag = depositRdaBagConverter.convert(deposit);

        var outputWriter = new StdoutBagOutputWriter();

        // send rda bag to vault
        try {
            rdaBagWriter.write(rdaBag, outputWriter); //Path.of("/tmp/testoutput/"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // move deposit to outbox
        // copilot says:
        // depositMover.move(deposit, Path.of("/tmp/testoutput/"));

    }
}
