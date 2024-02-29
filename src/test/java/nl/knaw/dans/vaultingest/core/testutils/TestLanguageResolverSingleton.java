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
package nl.knaw.dans.vaultingest.core.testutils;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.deposit.CsvLanguageResolver;
import nl.knaw.dans.vaultingest.core.deposit.LanguageResolver;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

@Slf4j
public class TestLanguageResolverSingleton {
    private static LanguageResolver instance;

    public static LanguageResolver getInstance() throws Exception {
        if (instance == null) {
            try {
                var iso2 = TestLanguageResolverSingleton.class.getResource("/debug-etc/iso639-1-to-dv.csv");
                var iso3 = TestLanguageResolverSingleton.class.getResource("/debug-etc/iso639-2-to-dv.csv");
                assert iso2 != null;
                var path2 = Path.of(iso2.toURI().getPath());
                assert iso3 != null;
                var path3 = Path.of(iso3.toURI().getPath());

                instance = new CsvLanguageResolver(path2, path3);
            }
            catch (URISyntaxException | IOException e) {
                e.printStackTrace();
                log.error("Could not load csv", e);
                throw e;
            }
        }

        return instance;
    }
}
