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
package nl.knaw.dans.vaultingest.core;

import lombok.Builder;
import nl.knaw.dans.vaultingest.core.domain.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.DatasetCreator;
import nl.knaw.dans.vaultingest.core.domain.DatasetOrganization;
import nl.knaw.dans.vaultingest.core.domain.DatasetRelation;
import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.DepositBag;
import nl.knaw.dans.vaultingest.core.domain.Description;
import nl.knaw.dans.vaultingest.core.domain.OtherId;
import nl.knaw.dans.vaultingest.core.domain.PidMappings;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
public class CommonDeposit implements Deposit {

    private final String id;
    private final Document ddm;
    private final Document filesXml;
    private final CommonDepositProperties properties;
    private final DepositBag depositBag;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:profile/dc:title")
            .findFirst()
            .orElse(null);
    }

    @Override
    public Collection<String> getAlternativeTitles() {
        return XPathEvaluator.strings(ddm,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:title",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:alternative")
            .collect(Collectors.toList());
    }

    @Override
    public Collection<OtherId> getOtherIds() {
        var results = new ArrayList<OtherId>();

        // CIT003, data from bag
        depositBag.getMetadataValue("Has-Organizational-Identifier")
            .stream()
            .filter(value -> {
                var parts = value.split(":", 2);
                return parts.length == 2 && StringUtils.isNotBlank(parts[0]) && StringUtils.isNotBlank(parts[1]);
            })
            .map(value -> {
                var parts = value.split(":", 2);
                return OtherId.builder()
                    .agency(parts[0])
                    .value(parts[1])
                    .build();
            })
            .findFirst()
            .ifPresent(results::add);

        // CIT004, data from ddm
        XPathEvaluator.strings(ddm,
                "/ddm:DDM/ddm:dcmiMetadata/ddm:identifier[not(@xsi:type)]",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:identifier[not(@xsi:type)]")
            .map(identifier -> OtherId.builder()
                .value(identifier)
                .build()
            )
            .forEach(results::add);

        return results;
    }

    @Override
    public Collection<Description> getDescriptions() {
        // CIT009, profile / description

        var profileDescriptions = XPathEvaluator.strings(ddm,
            "/ddm:DDM/ddm:profile/dc:description",
            "/ddm:DDM/ddm:profile/dcterms:description"
        ).map(value -> Description.builder()
            .value(value.trim())
            .build()
        );

        // CIT011, dcmiMetadata / [tags]
        var dcmiDescriptions = XPathEvaluator.nodes(ddm,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:date",
                "/ddm:DDM/ddm:dcmiMetadata/dc:date",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateAccepted",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateCopyrighted",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateSubmitted",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:modified",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:issued",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:valid",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:coverage")
            .map(node -> Description.builder()
                .type(node.getLocalName())
                .value(node.getTextContent().trim())
                .build()
            );

        // CIT012, dcmiMetadata / description
        var dcmiDescription = XPathEvaluator.strings(ddm,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:description")
            .map(value -> Description.builder()
                .value(value.trim())
                .build()
            );

        var streams = Stream.concat(profileDescriptions,
            Stream.concat(dcmiDescriptions, dcmiDescription)
        );

        return streams.collect(Collectors.toList());
    }

    @Override
    public Collection<DatasetRelation> getAuthors() {
        var results = new ArrayList<DatasetRelation>();

        // CIT005
        XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:profile/dc:creator")
            .map(String::trim)
            .map(author -> DatasetCreator.builder()
                .name(author)
                .build()
            )
            .forEach(results::add);

        // CIT006
        XPathEvaluator.nodes(ddm,
                "/ddm:DDM/ddm:profile/dcx-dai:creatorDetails/dcx-dai:author")
            .map(this::parseAuthor)
            .forEach(results::add);

        // CIT007
        // TODO format identifiers
        XPathEvaluator.nodes(ddm,
                "/ddm:DDM/ddm:profile/dcx-dai:creatorDetails/dcx-dai:organization")
            .map(node -> DatasetOrganization.builder()
                .name(getFirstValue(node, "dcx-dai:name"))
                .isni(getFirstValue(node, "dcx-dai:ISNI"))
                .viaf(getFirstValue(node, "dcx-dai:VIAF"))
                .build())
            .forEach(results::add);

        return results;
    }

    private String getFirstValue(Node node, String expression) {
        return XPathEvaluator.strings(node, expression).map(String::trim).findFirst().orElse(null);
    }

    private DatasetAuthor parseAuthor(Node node) {
        return DatasetAuthor.builder()
            .titles(getFirstValue(node, "dcx-dai:titles"))
            .initials(getFirstValue(node, "dcx-dai:initials"))
            .insertions(getFirstValue(node, "dcx-dai:insertions"))
            .surname(getFirstValue(node, "dcx-dai:surname"))
            // todo format identifiers
            .dai(getFirstValue(node, "dcx-dai:DAI"))
            .isni(getFirstValue(node, "dcx-dai:ISNI"))
            .orcid(getFirstValue(node, "dcx-dai:ORCID"))
            .role(getFirstValue(node, "dcx-dai:role"))
            .organization(getFirstValue(node, "dcx-dai:organization/dcx-dai:name"))
            .build();
    }

    @Override
    public String getSubject() {
        return null;
    }

    @Override
    public String getRightsHolder() {
        return null;
    }

    @Override
    public DepositBag getBag() {
        return depositBag;
    }

    @Override
    public PidMappings getPidMappings() {
        var mappings = new PidMappings();
        mappings.addMapping(this.id, "data");

        for (var file : depositBag.getPayloadFiles()) {
            mappings.addMapping(file.getId(), file.getPath().toString());
        }

        return mappings;
    }
}
