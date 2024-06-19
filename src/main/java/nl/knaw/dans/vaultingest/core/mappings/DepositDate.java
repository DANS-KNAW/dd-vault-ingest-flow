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
package nl.knaw.dans.vaultingest.core.mappings;

import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Document;

import java.util.Optional;

public class DepositDate extends Base {
    private static final DateTimeFormatter yyyymmddPattern = DateTimeFormat.forPattern("YYYY-MM-dd");

    public static Optional<Statement> toRDF(Resource resource, Deposit deposit) {
        return toDepositDate(resource, getDepositDate(deposit.getDdm()));
    }

    static String getDepositDate(Document document) {
        return XPathEvaluator.strings(document, "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateSubmitted")
            .findFirst()
            .map(DepositDate::toYearMonthDayFormat)
            .orElse(null); //TODO : NOW
    }

    static String toYearMonthDayFormat(String text) {
        var date = DateTime.parse(text);
        return yyyymmddPattern.print(date);
    }

    static Optional<Statement> toDepositDate(Resource resource, String dipositDate) {
        return toBasicTerm(resource, DVCitation.dateOfDeposit, dipositDate);
    }
}
