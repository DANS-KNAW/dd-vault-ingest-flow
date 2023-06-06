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
package nl.knaw.dans.vaultingest.health;

import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.net.URI;

public class DansBagValidatorHealthCheck extends HealthCheck {
    private static final Logger log = LoggerFactory.getLogger(DansBagValidatorHealthCheck.class);

    private final Client httpClient;
    private final URI pingUrl;

    public DansBagValidatorHealthCheck(Client httpClient, URI pingUrl) {
        this.httpClient = httpClient;
        this.pingUrl = pingUrl;
    }

    @Override
    protected Result check() {
        log.debug("Checking that Dans Bag Validator is available");

        try (var response = httpClient.target(pingUrl)
                .request(MediaType.TEXT_PLAIN)
                .get()) {

            if (response.getStatus() != 200) {
                var content = response.readEntity(String.class);

                if (!"pong".equals(content.trim())) {
                    throw new RuntimeException("Validate DANS bag ping URL did not respond with 'pong'");
                }

                throw new RuntimeException(String.format(
                        "Connection to Validate DANS Bag Service could not be established. Service responded with %s",
                        response.getStatusInfo()));
            }
        }
        catch (Throwable e) {
            return Result.unhealthy("Dans Bag Validator is not available: %s", e.getMessage());
        }

        return Result.healthy();
    }
}
