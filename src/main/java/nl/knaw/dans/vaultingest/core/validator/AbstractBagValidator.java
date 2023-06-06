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
package nl.knaw.dans.vaultingest.core.validator;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.validatedansbag.api.ValidateCommand;
import nl.knaw.dans.validatedansbag.api.ValidateOk;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractBagValidator implements BagValidator {
    private final Client httpClient;
    private final URI serviceUri;

    public AbstractBagValidator(Client httpClient, URI serviceUri) {
        this.httpClient = httpClient;
        this.serviceUri = serviceUri;
    }

    @Override
    public void validate(Path depositDir) throws InvalidBagException {
        var bagDir = getBagDir(depositDir);

        var command = new ValidateCommand()
                .bagLocation(bagDir.toString())
                .packageType(getPackageType());

        log.debug("Validating bag {} with command {}", bagDir, command);

        try (var multipart = new FormDataMultiPart()
                .field("command", command, MediaType.APPLICATION_JSON_TYPE)) {

            try (var response = httpClient.target(serviceUri)
                    .request()
                    .post(Entity.entity(multipart, multipart.getMediaType()))) {

                if (response.getStatus() == 200) {
                    var entity = response.readEntity(ValidateOk.class);
                    throw formatValidationError(entity);
                }
                else {
                    throw new RuntimeException(String.format(
                            "DANS Bag Validation failed (%s): %s",
                            response.getStatusInfo(), response.readEntity(String.class)));
                }
            }
        }
        catch (IOException e) {
            log.error("Unable to create multipart form data object", e);
        }
    }

    private InvalidBagException formatValidationError(ValidateOk result) {
        var violations = result.getRuleViolations().stream()
                .map(r -> String.format("- [%s] %s", r.getRule(), r.getViolation()))
                .collect(Collectors.joining("\n"));

        return new InvalidBagException(String.format(
                "Bag was not valid according to Profile Version %s. Violations: %s",
                result.getProfileVersion(), violations)
        );
    }

    protected abstract ValidateCommand.PackageTypeEnum getPackageType();

    protected Path getBagDir(Path path) throws InvalidBagException {
        try (var list = Files.list(path)) {
            return list.filter(Files::isDirectory)
                    .findFirst()
                    .orElseThrow();
        }
        catch (IOException e) {
            throw new InvalidBagException("Unable to find bag directory", e);
        }
    }
}
