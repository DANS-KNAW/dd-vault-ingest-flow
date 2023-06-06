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
import nl.knaw.dans.vaultingest.core.deposit.mapping.CollectionDates;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Contributors;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Creator;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Descriptions;
import nl.knaw.dans.vaultingest.core.deposit.mapping.DistributionDate;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Distributors;
import nl.knaw.dans.vaultingest.core.deposit.mapping.GrantNumbers;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Keywords;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Languages;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Organizations;
import nl.knaw.dans.vaultingest.core.deposit.mapping.OtherIds;
import nl.knaw.dans.vaultingest.core.deposit.mapping.PersonalData;
import nl.knaw.dans.vaultingest.core.deposit.mapping.ProductionDate;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Publications;
import nl.knaw.dans.vaultingest.core.deposit.mapping.RightsHolders;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Series;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Sources;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Subjects;
import nl.knaw.dans.vaultingest.core.deposit.mapping.Title;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.metadata.CollectionDate;
import nl.knaw.dans.vaultingest.core.domain.metadata.Contributor;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetContact;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetRelation;
import nl.knaw.dans.vaultingest.core.domain.metadata.Description;
import nl.knaw.dans.vaultingest.core.domain.metadata.Distributor;
import nl.knaw.dans.vaultingest.core.domain.metadata.GrantNumber;
import nl.knaw.dans.vaultingest.core.domain.metadata.Keyword;
import nl.knaw.dans.vaultingest.core.domain.metadata.OtherId;
import nl.knaw.dans.vaultingest.core.domain.metadata.Publication;
import nl.knaw.dans.vaultingest.core.domain.metadata.SeriesElement;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuperBuilder
class CommonDeposit implements Deposit {

    // for the migration deposit
    protected final Document ddm;
    protected final Document filesXml;
    protected final CommonDepositBag bag;
    private final String id;
    private final CommonDepositProperties properties;
    private final DatasetContactResolver datasetContactResolver;
    private final LanguageResolver languageResolver;
    private final List<DepositFile> depositFiles;
    private final Path path;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDoi() {
        return this.getProperty("identifier.doi");
    }

    @Override
    public String getNbn() {
        return this.getProperty("identifier.urn");
    }

    @Override
    public void setNbn(String nbn) {
        this.setProperty("identifier.urn", nbn);
    }

    @Override
    public String getTitle() {
        return Title.getTitle(ddm);
    }

    @Override
    public boolean isUpdate() {
        return false;
        // TODO remove false and uncomment the following line when the vault catalog integration is done
        //        return bag.getMetadataValue("Is-Version-Of").size() > 0;
    }

    @Override
    public String getSwordToken() {
        return null;
    }

    @Override
    public String getDepositorId() {
        return null;
    }

    @Override
    public State getState() {
        return this.getProperty("state.label") != null ? State.valueOf(this.getProperty("state.label")) : null;
    }

    @Override
    public void setState(State state, String message) {
        setProperty("state.label", state.name());
        setProperty("state.description", message);
    }

    @Override
    public Collection<String> getAlternativeTitles() {
        return Title.getAlternativeTitles(ddm);
    }

    @Override
    public Collection<OtherId> getOtherIds() {
        return OtherIds.getOtherIds(ddm, getMetadataValue("Has-Organizational-Identifier"));
    }

    @Override
    public Collection<Description> getDescriptions() {
        return Descriptions.getDescriptions(ddm);
    }

    @Override
    public Collection<DatasetRelation> getAuthors() {
        var results = new ArrayList<DatasetRelation>();

        // CIT005
        results.addAll(Creator.getCreators(ddm));

        // CIT006
        results.addAll(Author.getAuthors(ddm));

        // CIT007
        results.addAll(Organizations.getOrganizations(ddm));

        return results;
    }

    @Override
    public Collection<String> getSubjects() {
        return Subjects.getSubjects(ddm);
    }

    @Override
    public Collection<String> getRightsHolder() {
        return RightsHolders.getOtherIds(ddm);
    }

    @Override
    public Collection<Keyword> getKeywords() {
        return Keywords.getKeywords(ddm);
    }

    @Override
    public Collection<Publication> getPublications() {
        return Publications.getPublications(ddm);
    }

    @Override
    public Collection<String> getLanguages() {
        return Languages.getLanguages(ddm, languageResolver);
    }

    @Override
    public String getProductionDate() {
        return ProductionDate.getProductionDate(ddm);
    }

    @Override
    public Collection<Contributor> getContributors() {
        return Contributors.getContributors(ddm);
    }

    @Override
    public Collection<GrantNumber> getGrantNumbers() {
        return GrantNumbers.getGrantNumbers(ddm);
    }

    @Override
    public Collection<Distributor> getDistributors() {
        return Distributors.getDistributors(ddm);
    }

    @Override
    public String getDistributionDate() {
        return DistributionDate.getDistributionDate(ddm);
    }

    @Override
    public Collection<CollectionDate> getCollectionDates() {
        return CollectionDates.getCollectionDates(ddm);
    }

    @Override
    public Collection<SeriesElement> getSeries() {
        return Series.getSeries(ddm);
    }

    @Override
    public Collection<String> getSources() {
        return Sources.getSources(ddm);
    }

    @Override
    public DatasetContact getContact() {
        return datasetContactResolver.resolve(this.getProperty("depositor.userId"));
    }

    @Override
    public boolean isPersonalDataPresent() {
        return PersonalData.isPersonalDataPresent(ddm);
    }

    @Override
    public Collection<String> getMetadataLanguages() {
        return Languages.getMetadataLanguages(ddm, languageResolver);
    }

    @Override
    public Collection<DepositFile> getPayloadFiles() {
        return this.depositFiles;
    }

    @Override
    public Collection<Path> getMetadataFiles() throws IOException {
        return bag.getMetadataFiles();
    }

    @Override
    public InputStream inputStreamForMetadataFile(Path path) {
        return bag.inputStreamForMetadataFile(path);
    }

    private List<String> getMetadataValue(String key) {
        return bag.getMetadataValue(key);
    }

    protected String getProperty(String name) {
        return this.properties.getProperty(String.class, name);
    }

    protected void setProperty(String name, String value) {
        this.properties.setProperty(name, value);
    }

    Path getPath() {
        return path;
    }

    CommonDepositProperties getProperties() {
        return properties;
    }
}
