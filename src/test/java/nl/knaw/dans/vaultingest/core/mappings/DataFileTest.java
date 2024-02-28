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

import nl.knaw.dans.vaultingest.core.deposit.PayloadFile;
import nl.knaw.dans.vaultingest.core.xml.XmlNamespaces;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

import static org.assertj.core.api.Assertions.assertThat;

class DataFileTest {

    @Test
    void isRestricted_should_return_false_when_no_information_is_available() throws Exception {
        var depositFile = PayloadFile.builder()
            .filesXmlNode(getFilesXmlNode("data/invalid/characters/here:*?\"<>|;#.txt"))
            .ddmNode(getDdmNodeWithAccessRights(null))
            .build();

        assertThat(DataFile.isRestricted(depositFile.getFilesXmlNode(), depositFile.getDdmNode())).isFalse();
    }

    @Test
    void isRestricted_should_return_false_when_getAccessRights_equals_OPEN_ACCESS() throws Exception {
        var depositFile = PayloadFile.builder()
            .filesXmlNode(getFilesXmlNode("data/invalid/characters/here:*?\"<>|;#.txt"))
            .ddmNode(getDdmNodeWithAccessRights("OPEN_ACCESS"))
            .build();

        assertThat(DataFile.isRestricted(depositFile.getFilesXmlNode(), depositFile.getDdmNode())).isFalse();
    }

    @Test
    void isRestricted_should_return_true_when_getAccessRights_equals_RANDOM_VALUE() throws Exception {
        var depositFile = PayloadFile.builder()
            .filesXmlNode(getFilesXmlNode("data/invalid/characters/here:*?\"<>|;#.txt"))
            .ddmNode(getDdmNodeWithAccessRights("RANDOM_VALUE"))
            .build();

        assertThat(DataFile.isRestricted(depositFile.getFilesXmlNode(), depositFile.getDdmNode())).isTrue();
    }

    @Test
    void isRestricted_should_return_true_when_getAccessibleToRights_is_empty() throws Exception {
        var depositFile = PayloadFile.builder()
            .filesXmlNode(getFilesXmlNodeWithAccessibleToRights(""))
            .ddmNode(getDdmNodeWithAccessRights(null))
            .build();

        assertThat(DataFile.isRestricted(depositFile.getFilesXmlNode(), depositFile.getDdmNode())).isTrue();
    }

    @Test
    void isRestricted_should_return_true_when_getAccessibleToRights_equals_ANYTHING() throws Exception {
        var depositFile = PayloadFile.builder()
            .filesXmlNode(getFilesXmlNodeWithAccessibleToRights("ANYTHING"))
            .ddmNode(getDdmNodeWithAccessRights(null))
            .build();

        assertThat(DataFile.isRestricted(depositFile.getFilesXmlNode(), depositFile.getDdmNode())).isTrue();
    }

    @Test
    void isRestricted_should_return_false_when_getAccessibleToRights_equals_ANONYMOUS() throws Exception {
        var depositFile = PayloadFile.builder()
            .filesXmlNode(getFilesXmlNodeWithAccessibleToRights("ANONYMOUS"))
            .ddmNode(getDdmNodeWithAccessRights(null))
            .build();

        assertThat(DataFile.isRestricted(depositFile.getFilesXmlNode(), depositFile.getDdmNode())).isFalse();
    }

    Node getFilesXmlNode(String path) throws Exception {
        var node = new XmlReader().readXmlString("<file  xmlns=\"http://easy.dans.knaw.nl/schemas/bag/metadata/files/\" />");
        node.getDocumentElement().setAttribute("filepath", path);

        return node.getDocumentElement();
    }

    Node getFilesXmlNodeWithAccessibleToRights(String accessibleToRights) throws Exception {
        var node = new XmlReader().readXmlString("<file xmlns=\"http://easy.dans.knaw.nl/schemas/bag/metadata/files/\" />");
        node.getDocumentElement().setAttribute("filepath", "path/to/file.txt");

        var acc = node.createElementNS(XmlNamespaces.NAMESPACE_FILES_XML, "accessibleToRights");
        acc.setTextContent(accessibleToRights);
        node.getDocumentElement().appendChild(acc);
        return node.getDocumentElement();
    }

    Node getDdmNodeWithAccessRights(String mode) throws Exception {
        var accessRights = mode != null ? "<ddm:accessRights>" + mode + "</ddm:accessRights>" : "";
        var str = "<ddm:DDM xmlns:ddm='http://schemas.dans.knaw.nl/dataset/ddm-v2/'>"
            + "    <ddm:profile>"
            + accessRights
            + "    </ddm:profile>"
            + "</ddm:DDM>";

        var node = new XmlReader().readXmlString(str);
        return node.getDocumentElement();
    }
}