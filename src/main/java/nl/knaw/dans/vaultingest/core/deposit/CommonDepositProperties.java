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

class CommonDepositProperties {
    private final Configuration configuration;
    private final FileBasedConfigurationBuilder<FileBasedConfiguration> builder;

    public CommonDepositProperties(FileBasedConfigurationBuilder<FileBasedConfiguration> builder) throws ConfigurationException {
        this.configuration = builder.getConfiguration();
        this.builder = builder;
    }

    public <T> T getProperty(Class<T> cls, String name) {
        return configuration.get(cls, name);
    }

    public void setProperty(String name, Object value) {
        configuration.setProperty(name, value);
    }

    public FileBasedConfigurationBuilder<FileBasedConfiguration> getBuilder() {
        return builder;
    }
}
