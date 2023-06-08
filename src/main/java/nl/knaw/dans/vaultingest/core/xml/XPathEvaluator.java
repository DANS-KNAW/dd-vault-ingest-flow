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
package nl.knaw.dans.vaultingest.core.xml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class XPathEvaluator implements XmlNamespaces {

    private static XPath xpath;

    private static XPath getXpath() {
        if (xpath == null) {
            xpath = XPathFactory
                .newInstance()
                .newXPath();

            final var namespaceMap = new HashMap<String, String>();
            namespaceMap.put("xml", NAMESPACE_XML);
            namespaceMap.put("dc", NAMESPACE_DC);
            namespaceMap.put("dcx-dai", NAMESPACE_DCX_DAI);
            namespaceMap.put("ddm", NAMESPACE_DDM);
            namespaceMap.put("dcterms", NAMESPACE_DCTERMS);
            namespaceMap.put("xsi", NAMESPACE_XSI);
            namespaceMap.put("id-type", NAMESPACE_ID_TYPE);
            namespaceMap.put("dcx-gml", NAMESPACE_DCX_GML);
            namespaceMap.put("files", NAMESPACE_FILES_XML);
            namespaceMap.put("gml", NAMESPACE_OPEN_GIS);
            namespaceMap.put("wfs", NAMESPACE_EASY_WORKFLOW);
            namespaceMap.put("damd", NAMESPACE_DAMD);
            namespaceMap.put("agreements", NAMESPACE_AGREEMENTS);
            namespaceMap.put("afm", NAMESPACE_AFM);
            namespaceMap.put("datacite", NAMESPACE_DATACITE);

            xpath.setNamespaceContext(new NamespaceContext() {

                @Override
                public String getNamespaceURI(String s) {
                    return namespaceMap.get(s);
                }

                @Override
                public String getPrefix(String s) {
                    return null;
                }

                @Override
                public Iterator<String> getPrefixes(String s) {
                    return null;
                }
            });
        }

        return xpath;
    }

    public static Stream<Node> nodes(Node node, String... expressions) {
        try {
            return xpathsToStream(node, expressions);
        }
        catch (XPathExpressionException e) {
            var message = String.join(", ", expressions);
            throw new RuntimeException(String.format("Error evaluating xpath: %s", message), e);
        }
    }

    public static Stream<String> strings(Node node, String... expressions) {
        try {
            return xpathsToStreamOfStrings(node, expressions);
        }
        catch (XPathExpressionException e) {
            var message = String.join(", ", expressions);
            throw new RuntimeException(String.format("Error evaluating xpath: %s", message), e);
        }
    }

    private static synchronized Object evaluateXpath(Node node, String expr) throws XPathExpressionException {
        return getXpath().compile(expr).evaluate(node, XPathConstants.NODESET);
    }

    private static Stream<Node> xpathToStream(Node node, String expression) throws XPathExpressionException {
        var nodes = (NodeList) evaluateXpath(node, expression);

        return IntStream.range(0, nodes.getLength())
            .mapToObj(nodes::item);
    }

    private static Stream<Node> xpathsToStream(Node node, String... expressions) throws XPathExpressionException {
        var items = new ArrayList<Stream<Node>>();

        for (var expr: expressions) {
            var item = xpathToStream(node, expr);
            items.add(item);
        }

        return items.stream().flatMap(i -> i);
    }

    private static Stream<String> xpathsToStreamOfStrings(Node node, String... expressions) throws XPathExpressionException {
        return xpathsToStream(node, expressions).map(Node::getTextContent);
    }
}
