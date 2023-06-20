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
package nl.knaw.dans.vaultingest.core.utilities;

import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.domain.Deposit;

import java.nio.file.Path;

public class TestDepositManager implements DepositManager {
    private final Deposit deposit;

    private boolean saveDepositCalled = false;
    private Deposit.State lastState = null;
    private String lastMessage = null;

    public TestDepositManager(Deposit deposit) {
        this.deposit = deposit;
    }

    public Deposit.State getLastState() {
        return lastState;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    @Override
    public Deposit loadDeposit(Path path) {
        if (deposit == null) {
            throw new RuntimeException("No deposit set");
        }

        return deposit;
    }

    @Override
    public void saveDeposit(Deposit deposit) {
        saveDepositCalled = true;
    }

    @Override
    public void updateDepositState(Path path, Deposit.State state, String message) {
        lastState = state;
        lastMessage = message;
    }

    public boolean isSaveDepositCalled() {
        return saveDepositCalled;
    }
}
