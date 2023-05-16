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
package nl.knaw.dans.vaultingest.core.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestDeposit implements Deposit {
    private String id;
    private String title;
    private List<Description> descriptions;
    private List<DatasetRelation> authors;
    private String rightsHolder;
    private String subject;
    private DepositBag bag;
    private List<String> alternativeTitles;
    private List<OtherId> otherIds;
    private PidMappings pidMappings;
    private List<DepositFile> payloadFiles;
}
