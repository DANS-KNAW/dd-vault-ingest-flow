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
package nl.knaw.dans.vaultingest.core.domain;

import lombok.Builder;
import lombok.Data;
import nl.knaw.dans.vaultingest.core.domain.metadata.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@Data
@Builder
public class TestDeposit implements Deposit {
    private String id;
    private String title;
    private String nbn;
    private String doi;
    private List<Description> descriptions;
    private List<DatasetRelation> authors;
    private String rightsHolder;
    private String subject;
    private List<String> alternativeTitles;
    private List<OtherId> otherIds;
    private List<DepositFile> payloadFiles;

    private List<String> subjects;
    private List<Keyword> keywords;
    private List<Publication> publications;
    private List<String> languages;
    private String productionDate;

    private List<Contributor> contributors;
    private List<GrantNumber> grantNumbers;
    private List<Distributor> distributors;
    private String distributionDate;
    private List<CollectionDate> collectionDates;
    private List<SeriesElement> series;
    private DatasetContact contact;
    private List<String> sources;

//    @Override
//    public InputStream inputStreamForPayloadFile(DepositFile depositFile) {
//        return new ByteArrayInputStream(
//            String.format(
//                "This is a test payload file with id %s and path %s", depositFile.getId(), depositFile.getPath()
//            ).getBytes()
//        );
//    }
//
    @Override
    public Collection<Path> getMetadataFiles() throws IOException {
        return List.of(
            Path.of("bag-info.txt"),
            Path.of("bagit.txt"),
            Path.of("metadata/files.xml"),
            Path.of("metadata/dataset.xml")
        );
    }

    @Override
    public InputStream inputStreamForMetadataFile(Path path) {
        return new ByteArrayInputStream(
            String.format(
                "This is a test metadata file with path %s", path
            ).getBytes()
        );
    }
}
