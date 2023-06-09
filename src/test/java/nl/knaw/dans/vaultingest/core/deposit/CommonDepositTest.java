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

import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetContact;
import nl.knaw.dans.vaultingest.core.utilities.TestLanguageResolver;
import nl.knaw.dans.vaultingest.core.xml.XmlReaderImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommonDepositTest {

    @Test
    void getTitle() throws Exception {
        var deposit = this.loadDeposit();
        assertEquals("A bag containing examples for each mapping rule", deposit.getTitle());
    }

    @Test
    void getDescription() throws Exception {
        var deposit = this.loadDeposit();

        assertThat(deposit.getDescriptions())
            .extracting("value")
            .containsOnly(
                "This bags contains one or more examples of each mapping rule.",
                "A second description",
                "some date",
                "some acceptance date",
                "some copyright date",
                "some submission date",
                "some modified date",
                "some issuing date",
                "some validation date",
                "some coverage description",
                "Even more descriptions"
            );
        System.out.println(deposit.getDescriptions());
    }

    @Test
    void getOtherIds() throws Exception {
        var deposit = this.loadDeposit();

        assertThat(deposit.getOtherIds())
            .extracting("value")
            .containsOnly(
                "DCTERMS_ID001",
                "DCTERMS_ID002",
                "DCTERMS_ID003",
                "12345"
            );
    }

    @Test
    void getAuthors() throws Exception {
        var deposit = this.loadDeposit();
        var authors = deposit.getAuthors();
        assertThat(authors)
            .extracting("displayName")
            .containsOnly(
                "Unformatted Creator",
                "I Lastname",
                "Creator Organization"
            );

        assertThat(authors)
            .map(m -> m.getIdentifier() == null ? null : m.getIdentifier().getScheme())
            .containsOnly(
                null,
                "ORCID",
                "VIAF"
            );

        assertThat(authors)
            .map(m -> m.getIdentifier() == null ? null : m.getIdentifier().getValue())
            .containsOnly(
                null,
                "0000-1111-2222-3333",
                "123456789"
            );
    }

    @Test
    void getSubjects() throws Exception {
        var deposit = this.loadDeposit();
        var subjects = deposit.getSubjects();

        assertThat(subjects)
            .containsOnly("Chemistry", "Computer and Information Science");
    }

    @Test
    void getKeywords() throws Exception {
        var deposit = this.loadDeposit();
        var keywords = deposit.getKeywords();

        assertThat(keywords)
            .extracting("text")
            .containsOnly(
                "keyword1",
                "keyword2",
                "non-military uniform button",
                "Broader Match: buttons (fasteners)",
                "Old School Latin");

        assertThat(keywords)
            .extracting("vocabulary")
            .containsOnly(
                null,
                "PAN thesaurus ideaaltypes",
                "Art and Architecture Thesaurus"
            );

        assertThat(keywords)
            .extracting("vocabularyUri")
            .containsOnly(
                null,
                "https://data.cultureelerfgoed.nl/term/id/pan/PAN",
                "http://vocab.getty.edu/aat/"
            );
    }

    @Test
    void getPublications() throws Exception {
        var deposit = this.loadDeposit();
        var publications = deposit.getPublications();

        assertThat(publications)
            .extracting("idType")
            .containsOnly(
                "ISSN"
            );
        assertThat(publications)
            .extracting("idNumber")
            .containsOnly(
                "0317-8471"
            );
    }

    @Test
    void getLanguage() throws Exception {
        var deposit = this.loadDeposit();
        var languages = deposit.getLanguages();

        assertThat(languages)
            .containsOnly("Western Frisian", "Kalaallisut, Greenlandic", "Basque");
    }

    @Test
    void getProductionDate() throws Exception {
        var deposit = this.loadDeposit();
        var date = deposit.getProductionDate();

        assertEquals("2015-09-09", date);
    }

    @Test
    void getContributors() throws Exception {
        var deposit = this.loadDeposit();
        var contributors = deposit.getContributors();

        assertThat(contributors)
            .extracting("name")
            .containsOnly(
                "CON van Tributor (Contributing Org)",
                "Contributing Org"
            );

        assertThat(contributors)
            .extracting("type")
            .containsOnly(
                "ProjectMember", "Sponsor"
            );
    }

    @Test
    void getGrantNumbers() throws Exception {
        var deposit = this.loadDeposit();
        var grantNumbers = deposit.getGrantNumbers();

        assertThat(grantNumbers)
            .extracting("value")
            .containsOnly("54321");

        assertThat(grantNumbers)
            .extracting("agency")
            .containsOnly("NWO");
    }

    @Test
    void getDistributors() throws Exception {
        var deposit = this.loadDeposit();
        var distributors = deposit.getDistributors();

        assertThat(distributors)
            .extracting("name")
            .containsOnly(
                "D. I. Stributor"
            );
    }

    @Test
    void getDistributionDate() throws Exception {
        var deposit = this.loadDeposit();
        var distributionDate = deposit.getDistributionDate();

        assertEquals("2014-09-09", distributionDate);
    }

    @Test
    void getCollectionDates() throws Exception {
        var deposit = this.loadDeposit();
        var dates = deposit.getCollectionDates();

        assertThat(dates)
            .extracting("start")
            .containsOnly(
                "2015-06-01"
            );

        assertThat(dates)
            .extracting("end")
            .containsOnly(
                "2016-12-31"
            );
    }

    @Test
    void getSeries() throws Exception {
        var deposit = this.loadDeposit();
        var dates = deposit.getSeries();

        assertThat(dates)
            .extracting("information")
            .containsOnly(
                "Information about a series: first", "Information about a series: second"
            );
    }

    @Test
    void getSources() throws Exception {
        var deposit = this.loadDeposit();
        var sources = deposit.getSources();

        assertThat(sources)
            .containsOnly(
                "Sous an ayisyen", "Source 2", "Source 3"
            );
    }

    @Test
    void rightsHolders() throws Exception {
        var deposit = this.loadDeposit();
        var values = deposit.getRightsHolder();

        assertThat(values)
            .containsOnly("I Lastname");
    }

    @Test
    void isPersonalDataPresent() throws Exception {
        var deposit = this.loadDeposit();
        var personalData = deposit.isPersonalDataPresent();

        assertFalse(personalData);
    }

    @Test
    void getMetadataLanguages() throws Exception {
        var deposit = this.loadDeposit();
        var sources = deposit.getMetadataLanguages();

        assertThat(sources)
            .containsOnly(
                "English", "Haitian, Haitian Creole", "Georgian"
            );
    }

    Deposit loadDeposit() throws Exception {
        var props = Mockito.mock(CommonDepositProperties.class);
        var bag = Mockito.mock(CommonDepositBag.class);

        Mockito.when(bag.getMetadataValue(Mockito.eq("Has-Organizational-Identifier")))
            .thenReturn(List.of("DANS:12345"));

        var ddm = new XmlReaderImpl().readXmlFile(
            Path.of(Objects.requireNonNull(getClass().getResource("/xml/example-ddm.xml")).getPath())
        );

        return CommonDeposit.builder()
            .id("id")
            .ddm(ddm)
            .bag(bag)
            .filesXml(null)
            .properties(props)
            .datasetContactResolver((userId -> DatasetContact.builder()
                .name(userId)
                .email(userId + "@test.com")
                .affiliation(userId + " university")
                .build()))
            .languageResolver(new TestLanguageResolver())
            .build();
    }
}