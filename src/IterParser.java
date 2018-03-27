/**
 * Created by isuca in work catalogue
 *
 * @date 29-Sep-2017
 * @time 19:34
 */

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IterParser {

    // Object that allows access to DOM node by it's path in document
    private final XPath xPath;

    /**
     * Constructor with namespaces uri's
     *
     * @param uriMap all namespaces uri's
     */
    IterParser(final Map<String, String> uriMap) {
        xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                return uriMap.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
            }

            // Not required
            @Override
            public String getPrefix(String uri) {
                throw new UnsupportedOperationException();
            }

            // Not required
            @Override
            public Iterator getPrefixes(String uri) {
                throw new UnsupportedOperationException();
            }
        });

    }

    /**
     * Creates .xml-file tree representation
     *
     * @param pathToXmlFile path to .xml
     * @return tree file representation
     */
    Document createXmlDocument(String pathToXmlFile) {
        Document doc = null;
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            doc = builder.parse(pathToXmlFile);
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            Logger.getLogger(IterParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return doc;
    }

    /**
     * Returns list of nodes that correspond to the expression
     *
     * @param doc             .xml tree root
     * @param xpathExpression expression which nodes refer to
     * @return list of nodes
     */
    private List<String> getListOfValues(Document doc, String xpathExpression) {
        List<String> nodeValues = new ArrayList<>();
        try {
            NodeList nodes = (NodeList) xPath.compile(xpathExpression).evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                nodeValues.add(nodes.item(i).getNodeName() + " " + nodes.item(i).getNodeValue());
            }
        } catch (XPathExpressionException e) {
            Logger.getLogger(IterParser.class.getName()).log(Level.SEVERE, null, e);
        }
        return nodeValues;
    }

    /**
     * Returns list of nodes located at this path
     *
     * @param doc             .xml tree root
     * @param xpathExpression path to nodes
     * @return list of nodes
     */
    NodeList getNode(Document doc, String xpathExpression) {
        NodeList node = null;
        try {
            node = (NodeList) xPath.compile(xpathExpression).evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            Logger.getLogger(IterParser.class.getName()).log(Level.SEVERE, null, e);
        }
        return node;
    }

    /**
     * Return concatenation of all values of nodes corresponding to this path
     *
     * @param doc             .xml tree root
     * @param xpathExpression path to nodes
     * @return values
     */
    String getNodeValue(Document doc, String xpathExpression) {
        StringBuilder nodeValue = new StringBuilder();
        try {
            XPathExpression expr = xPath.compile(xpathExpression);
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                nodeValue.append(nodes.item(i).getTextContent());
                if (i < nodes.getLength() - 1) {
                    nodeValue.append(", ");
                }
            }
        } catch (XPathExpressionException e) {
            Logger.getLogger(IterParser.class.getName()).log(Level.SEVERE, null, e);
        }
        return nodeValue.toString();
    }

    /**
     * Converts date string between two dates formats
     *
     * @param f1 input format
     * @param f2 output format
     * @param dt date string in input format
     * @return date string in output format
     */
    static String formatDate(String f1, String f2, String dt) {
        try {
            if (dt != null && !dt.equals("")) {
                SimpleDateFormat sdfSource = new SimpleDateFormat(f1);
                Date date = sdfSource.parse(dt);
                SimpleDateFormat sdfDestination = new SimpleDateFormat(f2);
                dt = sdfDestination.format(date);
            }
        } catch (ParseException e) {
            Logger.getLogger(IterParser.class.getName()).log(Level.SEVERE, null, e);
        }
        return dt;
    }

    /**
     * Procedure for IterParser testing
     * Prints all tags and parameters in .xml file with depth formatting
     *
     * @param current current node
     * @param level   current node depth in tree
     */
    private void retrieveNodes(Node current, int level) {
        for (int i = 0; i < level; i++) {
            System.out.print("  ");
        }
        System.out.print(current.getNodeName() + "[");
        NamedNodeMap attributes = current.getAttributes();
        for (int c = 0; c < attributes.getLength(); c++) {
            Node attrib = attributes.item(c);
            System.out.print(attrib.getNodeName() + ": " + attrib.getNodeValue() + ", ");
        }
        System.out.println("]");

        NodeList children = current.getChildNodes();
        for (int c = 0; c < children.getLength(); c++) {
            Node child = children.item(c);
            if (child != null && child.getNodeType() == Node.ELEMENT_NODE) {
                retrieveNodes(child, level + 1);
            }
        }
    }

    /**
     * Start point for local debugging
     *
     * @param args cmd arguments
     */
    public static void main(String[] args) {
        final Map<String, String> myUriMaps = new HashMap<String, String>() {{
            put("xml", XMLConstants.XML_NS_URI);
            put("ns1", "urn://x-artefacts-fns-vipul-tosmv-ru/311-14/4.0.5");
            put("fnst", "urn://x-artefacts-fns/vipul-types/4.0.5");
        }};

        final IterParser parser = new IterParser(myUriMaps);

        final Document document = parser.createXmlDocument("files/egrul.xml");
        document.getDocumentElement().normalize();

        CSVParser csvParser = new CSVParser("files/egrul.csv", "files/egrul.json");
        CSVParser.TreeNode root = csvParser.convertFromCSV();
        csvParser.printToJSON(root);
        csvParser.close();

        parser.retrieveNodes(document.getDocumentElement(), 0);
        List<String> allValues = parser.getListOfValues(document, "//*/@*|//*/text()[string-length(normalize-space(.))!=0]");
        for (final String s : allValues) {
            System.out.println(s);
        }
    }

}
