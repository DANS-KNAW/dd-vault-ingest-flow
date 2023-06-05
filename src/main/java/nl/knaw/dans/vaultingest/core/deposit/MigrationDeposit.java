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

import lombok.experimental.SuperBuilder;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Author;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Organizations;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetOrganization;
import org.w3c.dom.Document;

import java.util.Collection;
import java.util.stream.Collectors;

@SuperBuilder
public class MigrationDeposit extends CommonDeposit {
    private final Document agreementsXml;
    private final Document amdXml;

    @Override
    public Collection<String> getRightsHolder() {
        var result = super.getRightsHolder();

        // RIG000A
        result.addAll(Author.getAuthors(ddm)
            .stream()
            .filter(DatasetAuthor::isRightsHolder)
            .map(DatasetAuthor::getRightsHolderDisplayName)
            .collect(Collectors.toList()));

        // RIG000B
        result.addAll(Organizations.getOrganizations(ddm)
            .stream()
            .filter(DatasetOrganization::isRightsHolder)
            .map(DatasetOrganization::getDisplayName)
            .collect(Collectors.toList()));

        return result;
    }
}
