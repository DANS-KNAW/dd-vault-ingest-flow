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
import nl.knaw.dans.validatedansbag.api.ValidateCommandDto;
import nl.knaw.dans.validatedansbag.api.ValidateOkDto;
import nl.knaw.dans.vaultingest.core.validator.BagValidator;
import nl.knaw.dans.vaultingest.core.validator.InvalidDepositException;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
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
    public void validate(Path bagDir) throws InvalidDepositException, IOException {
        if (bagDir == null) {
            throw new InvalidDepositException("Bag directory cannot be null");
        }

        var command = new ValidateCommandDto()
            .bagLocation(bagDir.toString())
            .packageType(getPackageType());

        log.debug("Validating bag {} with command {}", bagDir, command);

        try (var multipart = new FormDataMultiPart()
            .field("command", command, MediaType.APPLICATION_JSON_TYPE)) {

            var response = validateMultipartObject(multipart);

            if (Boolean.FALSE.equals(response.getIsCompliant())) {
                throw formatValidationError(response);
            }
        }
        catch (IOException e) {
            log.error("Unexpected error while communicating with bag validator on url {}", serviceUri, e);
            throw e;
        }
    }

    private InvalidDepositException formatValidationError(ValidateOkDto result) {
        if (result.getRuleViolations() == null) {
            return new InvalidDepositException("Bag was not valid according to Profile Version " + result.getProfileVersion() + ", but no violations were reported");
        }

        var violations = result.getRuleViolations().stream()
            .map(r -> String.format("- [%s] %s", r.getRule(), r.getViolation()))
            .collect(Collectors.joining("\n"));

        return new InvalidDepositException(String.format(
            "Bag was not valid according to Profile Version %s. Violations: \n%s",
            result.getProfileVersion(), violations)
        );
    }

    ValidateOkDto validateMultipartObject(MultiPart multipart) throws IOException {
        try (var response = makeRequest(multipart)) {
            log.debug("Validate bag response: {}", response);

            if (response.getStatus() != 200) {
                throw new IOException(String.format(
                    "Unexpected response from bag validator service: %s %s",
                    response.getStatus(), response.getStatusInfo().getReasonPhrase())
                );
            }

            return response.readEntity(ValidateOkDto.class);
        }
    }

    Response makeRequest(MultiPart multipart) throws IOException {
        try {
            var response = httpClient.target(serviceUri)
                .request()
                .post(Entity.entity(multipart, multipart.getMediaType()));

            log.debug("Validate bag response: {}", response);
            return response;
        }
        catch (ProcessingException e) {
            throw new IOException("Unexpected response from bag validator service", e);
        }
    }

    protected abstract ValidateCommandDto.PackageTypeEnum getPackageType();

}
