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

import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadFileTest {

    @Test
    void getDirectoryLabel_should_return_same_path_for_valid_characters() throws Exception {
        var depositFile = PayloadFile.builder()
            .filesXmlNode(getFilesXmlNode("data/only/valid/characters.txt"))
            .build();

        assertThat(depositFile.getDirectoryLabel()).isEqualTo(Path.of("data/only/valid/"));
    }

    @Test
    void getFilename_should_return_same_value_for_valid_characters() throws Exception {
        var depositFile = PayloadFile.builder()
            .filesXmlNode(getFilesXmlNode("data/valid/characters.txt"))
            .build();

        assertThat(depositFile.getFilename()).isEqualTo(Path.of("characters.txt"));
    }

    Node getFilesXmlNode(String path) throws Exception {
        var node = new XmlReader().readXmlString("<file xmlns=\"http://easy.dans.knaw.nl/schemas/bag/metadata/files/\" />");
        node.getDocumentElement().setAttribute("filepath", path);

        return node.getDocumentElement();
    }

}