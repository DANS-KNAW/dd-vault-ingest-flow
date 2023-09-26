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
package nl.knaw.dans.vaultingest.client;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultcatalog.api.OcflObjectVersionDto;
import nl.knaw.dans.vaultcatalog.api.OcflObjectVersionParametersDto;
import nl.knaw.dans.vaultcatalog.client.ApiException;
import nl.knaw.dans.vaultcatalog.client.OcflObjectVersionApi;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogDeposit;
import nl.knaw.dans.vaultingest.core.vaultcatalog.VaultCatalogRepository;

import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class VaultCatalogClient implements VaultCatalogRepository {
    private final OcflObjectVersionApi ocflObjectVersionApi;

    public VaultCatalogClient(OcflObjectVersionApi ocflObjectVersionApi) {
        this.ocflObjectVersionApi = ocflObjectVersionApi;
    }

    @Override
    public VaultCatalogDeposit registerDeposit(Deposit deposit) throws IOException {
        var bagId = deposit.getBagId();

        // find latest version
        try {
            // find highest version
            // note that in the vault ingest flow, currently there should never be an existing version
            // so the highestVersion variable should always be 0
            var highestVersion = findHighestVersion(bagId);

            // register with n+1
            var parameters = new OcflObjectVersionParametersDto()
                .skeletonRecord(true)
                .nbn(deposit.getNbn())
                .dataSupplier(deposit.getDataSupplier())
                .swordToken(deposit.getSwordToken());

            var newVersion = highestVersion + 1;
            var response = ocflObjectVersionApi.createOcflObjectVersion(bagId, (int)newVersion, parameters);

            log.debug("Registered deposit, response: {}", response);


            return VaultCatalogDeposit.builder()
                .objectVersion(newVersion)
                .bagId(bagId)
                .nbn(response.getNbn())
                .dataSupplier(response.getDataSupplier())
                .build();
        }
        catch (ApiException e) {
            log.error("Error while registering deposit: {}", e.getMessage(), e);
            throw new IOException(e.getResponseBody(), e);
        }
    }

    @Override
    public Optional<VaultCatalogDeposit> findDeposit(String swordToken) throws IOException {
        if (swordToken == null) {
            return Optional.empty();
        }

        try {
            var latestVersion = ocflObjectVersionApi.getOcflObjectsBySwordToken(swordToken)
                .stream()
                .max(Comparator.comparingInt(OcflObjectVersionDto::getObjectVersion));

            return latestVersion
                .map(item -> {
                    var nbn = item.getNbn();
                    var dataSupplier = item.getDataSupplier();

                    return VaultCatalogDeposit.builder()
                        .dataSupplier(dataSupplier)
                        .nbn(nbn)
                        .build();
                });
        }
        catch (ApiException e) {
            if (e.getCode() == 404) {
                return Optional.empty();
            }

            log.error("Error while registering deposit: {}", e.getMessage(), e);
            throw new IOException(e.getResponseBody(), e);
        }
    }

    long findHighestVersion(String bagId) {
        try {
            var versions = ocflObjectVersionApi.getOcflObjectsByBagId(bagId);

            // find the highest version
            // note that in the vault ingest flow, currently there should never be an existing version
            // so the highestVersion variable should always be 0
            return versions.stream()
                .mapToInt(OcflObjectVersionDto::getObjectVersion)
                .max()
                .orElse(0);
        }
        catch (ApiException e) {
            if (e.getCode() == 404) {
                return 0;
            }

            log.error("Error while reading vault catalog: {}", e.getResponseBody(), e);
            throw new RuntimeException(e.getResponseBody(), e);
        }
    }
}
