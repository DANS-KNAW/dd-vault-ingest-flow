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

import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nl.knaw.dans.vaultingest.core.deposit.mapping.*;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.DepositFile;
import nl.knaw.dans.vaultingest.core.domain.metadata.*;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import nl.knaw.dans.vaultingest.core.xml.XmlNamespaces;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@SuperBuilder
@ToString
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
    private String nbn;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDoi() {
        var prefix = ddm.lookupPrefix(XmlNamespaces.NAMESPACE_ID_TYPE);
        var expr = String.format("/ddm:DDM/ddm:dcmiMetadata/dcterms:identifier[@xsi:type='%s:DOI']", prefix);
        var dois = XPathEvaluator.strings(ddm, expr).collect(Collectors.toList());

        if (dois.size() != 1) {
            throw new IllegalStateException("There should be exactly one DOI in the DDM, but found " + dois.size() + " DOIs");
        }

        var doi = dois.get(0);

        if (StringUtils.isBlank(doi)) {
            throw new IllegalStateException("DOI is blank in the DDM");
        }

        return doi;
    }

    @Override
    public String getNbn() {
        return this.nbn;
    }

    @Override
    public void setNbn(String nbn) {
        this.nbn = nbn;
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
        return this.properties.getDepositorId();
    }

    public State getState() {
        var label = this.properties.getStateLabel();
        return label != null ? State.valueOf(label) : null;
    }

    @Override
    public void setState(State state, String message) {
        this.properties.setStateLabel(state.name());
        this.properties.setStateDescription(message);
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
        return datasetContactResolver.resolve(
            this.properties.getDepositorId()
        );
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

    Path getPath() {
        return path;
    }

    CommonDepositProperties getProperties() {
        return properties;
    }
}
