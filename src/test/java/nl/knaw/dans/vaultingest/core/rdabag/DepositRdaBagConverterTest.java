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
package nl.knaw.dans.vaultingest.core.rdabag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.dans.vaultingest.core.domain.DatasetAuthor;
import nl.knaw.dans.vaultingest.core.domain.Description;
import nl.knaw.dans.vaultingest.core.domain.OreResourceMap;
import nl.knaw.dans.vaultingest.core.domain.TestDeposit;
import nl.knaw.dans.vaultingest.core.domain.ids.DAI;
import nl.knaw.dans.vaultingest.core.rdabag.DepositRdaBagConverter;
import nl.knaw.dans.vaultingest.domain.Resource;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepositRdaBagConverterTest {

    @Test
    void convert() throws Exception {
        var deposit = TestDeposit.builder()
            .id("doi:10.17026/dans-12345")
            .title("The beautiful title")
            .descriptions(List.of(
                Description.builder().value("Description 1").build(),
                Description.builder().value("Description 2").build()
            ))
            .authors(List.of(
                DatasetAuthor.builder()
                    .initials("EJ")
                    .name("Eric")
                    .affiliation("Affiliation 1")
                    .dai(new DAI("123456"))
                    .build()
            ))

            .subject("Something about science")
            .rightsHolder("John Rights")
            .build();

        System.out.println("DEPOSIT: " + deposit);
        var converter = new DepositRdaBagConverter();
        var bag = converter.convert(deposit);
        System.out.println("BAG: " + bag);

        assertEquals(deposit.getId(), bag.getId());

        //        printResource(bag.getResource());
        //        printOre(bag.getOreResourceMap());

    }

    void printResource(Resource resource) throws Exception {
        var context = JAXBContext.newInstance(Resource.class);
        var marshaller = context.createMarshaller();
        var strWriter = new StringWriter();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(resource, strWriter);

        System.out.println("RESOURCE: " + strWriter);
    }

    void printOre(OreResourceMap map) throws JsonProcessingException {
        var result = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(map);
        //        System.out.println("ORE: " + result);
    }
}