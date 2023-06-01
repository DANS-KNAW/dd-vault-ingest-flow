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
package nl.knaw.dans.vaultingest.core.inbox;

import nl.knaw.dans.vaultingest.core.DepositToBagProcess;
import nl.knaw.dans.vaultingest.core.deposit.CommonDepositFactory;

import java.nio.file.Path;

public class ProcessDepositTaskFactory {

    private final CommonDepositFactory depositFactory;
    private final DepositToBagProcess depositToBagProcess;

    public ProcessDepositTaskFactory(CommonDepositFactory depositFactory, DepositToBagProcess depositToBagProcess) {
        this.depositFactory = depositFactory;
        this.depositToBagProcess = depositToBagProcess;
    }

    ProcessDepositTask createProcessDepositTask(Path path, Path outboxPath) {
        return new ProcessDepositTask(
            depositFactory,
            depositToBagProcess,
            path,
            outboxPath
        );
    }
}
