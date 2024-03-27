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

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class DepositProperties {
    private static final String NBN = "identifier.urn";
    private static final String DATAVERSE_BAG_ID = "dataverse.bag-id";
    private static final String DATAVERSE_NBN = "dataverse.nbn";
    private static final String STATE_LABEL = "state.label";
    private static final String STATE_DESCRIPTION = "state.description";
    private static final String IDENTIFIER_DOI = "identifier.doi";
    private static final String DEPOSITOR_ID = "depositor.userId";
    private static final String DATAVERSE_SWORD_TOKEN = "dataverse.sword-token";

    private static final String CREATION_TIMESTAMP = "creation.timestamp";

    private final Configuration configuration;
    private final FileBasedConfigurationBuilder<FileBasedConfiguration> builder;

    public DepositProperties(FileBasedConfigurationBuilder<FileBasedConfiguration> builder) throws ConfigurationException {
        this.configuration = builder.getConfiguration();
        this.builder = builder;
    }

    public String getNbn() {
        return configuration.getString(NBN);
    }

    public void setNbn(String nbn) {
        configuration.setProperty(NBN, nbn);
    }

    public String getStateLabel() {
        return configuration.getString(STATE_LABEL);
    }

    public void setStateLabel(String stateLabel) {
        configuration.setProperty(STATE_LABEL, stateLabel);
    }

    public String getStateDescription() {
        return configuration.getString(STATE_DESCRIPTION);
    }

    public void setStateDescription(String stateDescription) {
        configuration.setProperty(STATE_DESCRIPTION, stateDescription);
    }

    public String getIdentifierDoi() {
        return configuration.getString(IDENTIFIER_DOI);
    }

    public void setIdentifierDoi(String identifierDoi) {
        configuration.setProperty(IDENTIFIER_DOI, identifierDoi);
    }

    public String getDataverseNbn() {
        return configuration.getString(DATAVERSE_NBN);
    }

    public String getDepositorId() {
        return configuration.getString(DEPOSITOR_ID);
    }

    public void setDepositorId(String depositorId) {
        configuration.setProperty(DEPOSITOR_ID, depositorId);
    }

    public void save() throws ConfigurationException {
        builder.save();
    }

    public String getBagId() {
        return configuration.getString(DATAVERSE_BAG_ID);
    }

    public String getSwordToken() {
        return configuration.getString(DATAVERSE_SWORD_TOKEN);
    }

    public String getCreationTimestamp() {
        return configuration.getString(CREATION_TIMESTAMP);
    }
}
