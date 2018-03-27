/**
 * Created by isuca in work catalogue
 *
 * @date 13-Oct-17
 * @time 19:45
 */

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

class PdfCreatorTools {

    // Fonts
    static Font fPlain, fHeader, fLevel[], fSign;
    // Table with document values
    private PdfPTable mainTable;
    // Current row number
    private int last;

    PdfCreatorTools(String pathToFont) {
        try {
            BaseFont times = BaseFont.createFont(pathToFont, "cp1251", BaseFont.EMBEDDED);
            fPlain = new Font(times, 10);
            fHeader = new Font(times, 14, Font.BOLD);
            fLevel = new Font[]{
                    new Font(times, 12, Font.BOLD),
                    new Font(times, 10, Font.BOLD)
            };
            fSign = new Font(times, 6, Font.NORMAL);

            mainTable = new PdfPTable(3);
            mainTable.setWidthPercentage(100);
            mainTable.setWidths(new int[]{1, 8, 8});
            mainTable.setSpacingBefore(20);
            mainTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            mainTable.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
            last = 0;
        } catch (DocumentException | IOException e) {
            Logger.getLogger(PdfCreatorTools.class.getName()).log(Level.SEVERE, "Unable to create table", e);
        }
    }

    /**
     * Recursively generates all .pdf content declared in current node
     *
     * @param current current node/tag of .xml tree representation
     * @param rules   current node of layout structure tree
     * @param level   current node depth in the tree
     */
    void parseNode(Node current, CSVParser.TreeNode rules, int level) {
        // Return if there is no node
        if (rules == null) {
            Logger.getLogger(PdfCreatorEgrul.class.getName()).log(Level.SEVERE, "Current node is null", 0);
            return;
        }
        // Generates all required content that is somehow not presented in document
        if (current == null) {
            if (rules.isRequired()) {
                if (level == 0) {
                    mainTable.addCell(createSeparatorCell(rules.getName(), fHeader));
                } else if (level == 1) {
                    mainTable.addCell(createSeparatorCell(rules.getName(), fLevel[0]));
                } else if (level > 0) {
                    mainTable.addCell(createColspanCell(rules.getName(), fLevel[Math.min(fLevel.length - 1, level - 1)]));
                }
                mainTable.addCell(createColspanCell("Сведения не найдены", fPlain));
            }
            return;
        }
        // If node is tag then check it's content
        if (rules.isNode()) {
            NodeList nodes = current.getChildNodes();
            NamedNodeMap attrib = current.getAttributes();
            HashMap<String, ArrayList<Node>> nsNodes = new HashMap<>();

            // Every local name refers to list of nodes with this name
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String name = node.getLocalName();
                    if (!nsNodes.containsKey(name)) {
                        nsNodes.put(name, new ArrayList<>());
                    }
                    nsNodes.get(name).add(node);
                }
            }
            for (int i = 0; i < attrib.getLength(); i++) {
                Node item = attrib.item(i);
                nsNodes.put(item.getLocalName(), new ArrayList<>());
                nsNodes.get(item.getLocalName()).add(attrib.item(i));
            }

            boolean empty = true;
            if (level == 0) {
                mainTable.addCell(createSeparatorCell(rules.getName(), fHeader));
            } else if (level == 1) {
                mainTable.addCell(createSeparatorCell(rules.getName(), fLevel[0]));
            } else if (level > 1) {
                mainTable.addCell(createColspanCell(rules.getName(), fLevel[Math.min(fLevel.length - 1, level - 1)]));
            }

            // Check all data that is presented in layout rules
            for (CSVParser.TreeNode param : rules.getParams()) {
                if (nsNodes.getOrDefault(param.getPath(), null) != null) {
                    for (Node node : nsNodes.get(param.getPath())) {
                        empty = false;
                        String value = node.getNodeValue();
                        if (value == null) {
                            value = node.getTextContent();
                        }
                        if (param.getType().contains("date")) {
                            value = IterParser.formatDate("yyyy-MM-dd", "dd.MM.yyyy", value);
                        }
                        addRow(mainTable, ++last, param.getName(), value);
                    }
                }
            }
            // Check all data on lower levels
            for (CSVParser.TreeNode child : rules.getChildren()) {
                ArrayList<Node> next = nsNodes.getOrDefault(child.getPath(), null);
                if (next != null) {
                    empty = false;
                    for (Node node : next) {
                        parseNode(node, child, level + 1);
                    }
                }
            }
            // If the data is required and missing
            if (empty) {
                if (rules.isRequired()) {
                    mainTable.addCell(createColspanCell("Сведения не найдены", fPlain));
                } else {
                    mainTable.deleteLastRow();
                }
            }
        }
    }

    /**
     * Creates table separator cell
     *
     * @param separatorCellContent string content
     * @param font                 font
     * @return table cell with separation style
     */
    private PdfPCell createSeparatorCell(String separatorCellContent, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(separatorCellContent, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setPaddingBottom(7);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setColspan(3);
        return cell;
    }

    /**
     * Creates table colspan cell
     *
     * @param colspanCellContent string content
     * @param font               font
     * @return table cell with colspan style
     */
    private PdfPCell createColspanCell(String colspanCellContent, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(colspanCellContent, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setColspan(3);
        cell.setPaddingBottom(5);
        return cell;
    }

    /**
     * Adds three-element row into table
     *
     * @param table table
     * @param index row number (1 element)
     * @param name  parameter name (2 element)
     * @param value parameter value (3 element)
     */
    private void addRow(PdfPTable table, int index, String name, String value) {
        table.addCell(new Phrase(String.valueOf(index), fPlain));
        table.addCell(new Phrase(name, fPlain));
        table.addCell(new Phrase(value, fPlain));
    }

    PdfPTable getTable() {
        return mainTable;
    }
}
