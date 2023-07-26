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

import nl.knaw.dans.validatedansbag.api.ValidateOkDto;
import nl.knaw.dans.validatedansbag.api.ValidateOkRuleViolationsInnerDto;
import nl.knaw.dans.vaultingest.core.validator.InvalidDepositException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractBagValidatorTest {

    @Test
    void validate_should_not_throw_if_validator_returns_yields_correct_output() throws Exception {
        var client = Mockito.mock(Client.class);
        var validator = Mockito.spy(new BagValidator(client, URI.create("http://localhost/")));

        var result = new ValidateOkDto()
            .isCompliant(true);

        Mockito.doReturn(result).when(validator).validateMultipartObject(Mockito.any());

        assertThatNoException().isThrownBy(() -> validator.validate(Path.of("src/test/resources/bag")));
    }

    @Test
    void validate_should_throw_InvalidDepositException_when_bag_contains_errors() throws Exception {
        var client = Mockito.mock(Client.class);
        var validator = Mockito.spy(new BagValidator(client, URI.create("http://localhost/")));

        Mockito.doReturn(
                new ValidateOkDto()
                    .isCompliant(false)
                    .addRuleViolationsItem(new ValidateOkRuleViolationsInnerDto().rule("1.1").violation("error 1"))
            )
            .when(validator).validateMultipartObject(Mockito.any());

        assertThatThrownBy(() -> validator.validate(Path.of("src/test/resources/bag")))
            .isInstanceOf(InvalidDepositException.class)
            .hasMessageContaining("error 1");
    }

    @Test
    void validate_should_throw_IOException_when_request_has_errors() throws Exception {
        var client = Mockito.mock(Client.class);
        var validator = Mockito.spy(new BagValidator(client, URI.create("http://localhost/")));

        Mockito.doThrow(new IOException("error 1"))
            .when(validator).validateMultipartObject(Mockito.any());

        assertThatThrownBy(() -> validator.validate(Path.of("src/test/resources/bag")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("error 1");
    }
}