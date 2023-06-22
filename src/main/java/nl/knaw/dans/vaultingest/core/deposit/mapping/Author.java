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
package nl.knaw.dans.vaultingest.core.deposit.mapping;

import nl.knaw.dans.vaultingest.core.domain.Deposit;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetRelation;
import nl.knaw.dans.vaultingest.core.rdabag.converter.mappers.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.rdabag.converter.mappers.vocabulary.Datacite;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import nl.knaw.dans.vaultingest.domain.Affiliation;
import nl.knaw.dans.vaultingest.domain.NameIdentifier;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Author extends Base {
    public static List<DatasetAuthor> getAuthors(Document ddm) {
        return XPathEvaluator.nodes(ddm,
                "/ddm:DDM/ddm:profile/dcx-dai:creatorDetails/dcx-dai:author")
            .map(Author::parseAuthor)
            .collect(Collectors.toList());
    }

    static DatasetAuthor parseAuthor(Node node) {
        return DatasetAuthor.builder()
            .titles(getFirstValue(node, "dcx-dai:titles"))
            .initials(getFirstValue(node, "dcx-dai:initials"))
            .insertions(getFirstValue(node, "dcx-dai:insertions"))
            .surname(getFirstValue(node, "dcx-dai:surname"))
            .dai(getDAI(node))
            .isni(getISNI(node))
            .orcid(getORCID(node))
            .role(getFirstValue(node, "dcx-dai:role"))
            .affiliation(getFirstValue(node, "dcx-dai:organization/dcx-dai:name"))
            .build();
    }
}
