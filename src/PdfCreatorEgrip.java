/**
 * Created by isuca in work catalogue
 *
 * @date 13-Oct-17
 * @time 19:22
 */

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.xml.XMLConstants;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static rrp.PdfCreatorTools.*;

public class PdfCreatorEgrip {

    private final PdfCreatorTools creator;
    // Namespaces uri's
    private static final HashMap<String, String> namepaces = new HashMap<String, String>() {{
        put("xml", XMLConstants.NULL_NS_URI);
        put("ns1", "urn://x-artefacts-fns-vipip-tosmv-ru/311-15/4.0.5");
        put("fnst", "urn://x-artefacts-fns/vipip-types/4.0.5");
    }};

    /**
     * Constructor with path to font
     *
     * @param pathToFont      base font path
     * @param targetNamespace unused
     */
    public PdfCreatorEgrip(String pathToFont, String targetNamespace) {
        creator = new PdfCreatorTools(pathToFont);
    }

    /**
     * Creates full .pdf-file with all data
     *
     * @param doc           input .xml-file
     * @param pathToPdfFile path to output .pdf-file
     */
    public void createFullEgrip(org.w3c.dom.Document doc, String pathToPdfFile) {
        try {
            if (doc != null) {
                Document document = new Document(PageSize.A4, 45, 45, 45, 45);
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pathToPdfFile));

                // .xml file parser
                IterParser docParser = new IterParser(namepaces);

                // Layout description file parser
                CSVParser ruleParser = new CSVParser("files/egrip.csv", "files/egrip.json");

                CSVParser.TreeNode rules = ruleParser.convertFromCSV();
                ruleParser.printToJSON(rules);
                document.open();
                doc.getDocumentElement().normalize(); // Normalize document
                buildDocument(document, doc, docParser, rules);

                document.close();
                writer.close();
            }
        } catch (DocumentException | FileNotFoundException e) {
            Logger.getLogger(PdfCreatorEgrul.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Checks return code, and parses it if it's present
     * If not, creates header of document and calls parser from root node
     *
     * @param document  .pdf-file
     * @param doc       .xml-file
     * @param docParser IterParser object, allows to take values from .xml tags and parameters
     * @param rules     root of layout file tree
     */
    private void buildDocument(Document document, org.w3c.dom.Document doc, IterParser docParser, CSVParser.TreeNode rules) throws DocumentException {
        Paragraph p = new Paragraph("ВЫПИСКА", fHeader);
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(Chunk.NEWLINE);
        p.add(new Phrase("из Единого государственного реестра индивидуальных предпринимателей", fLevel[1]));
        document.add(p);

        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new int[]{1, 2, 3});
        headerTable.setSpacingBefore(30);
        headerTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
        headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        String date = IterParser.formatDate("yyyy-MM-dd", "dd.MM.yyyy", docParser.getNodeValue(doc, "//ns1:FNSVipIPResponse/ns1:СвЮЛ/@ДатаВып"));
        if (Objects.equals(date, "")) {
            date = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(java.time.LocalDate.now());
        }
        headerTable.addCell(new Paragraph("Дата: " + date, fPlain));
        headerTable.addCell("");
        PdfPCell cell = new PdfPCell(new Phrase("№: " + docParser.getNodeValue(doc, "//ns1:FNSVipIPResponse/@ИдДок"), fPlain));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(cell);
        document.add(headerTable);

        try {
            String requestCode = docParser.getNode(doc, "//ns1:FNSVipIPResponse/ns1:КодОбр").item(0).getTextContent();
            if (requestCode == null) {
                throw new NullPointerException("Fake exception, КодОбр not found");
            } else {
                p = new Paragraph();
                p.setAlignment(Element.ALIGN_CENTER);
                p.add(Chunk.NEWLINE);
                p.add(new Phrase("не может быть предоставлена:", fPlain));
                p.add(new Chunk("\n\n"));
                if (Objects.equals(requestCode, "01")) {
                    p.add(new Phrase("Сведения в отношении индивидуального предпринимателя в ЕГРИП по его ОГРНИП не найдены", fLevel[1]));
                } else if (Objects.equals(requestCode, "53")) {
                    p.add(new Phrase("Сведения в отношении индивидуального предпринимателя не могут быть предоставлены в электронном виде", fLevel[1]));
                } else {
                    // If there ever will be other return codes
                    p.add(new Phrase("При обработке запроса проиошла неизвестная ошибка, код обработки - " + requestCode, fLevel[1]));
                }
                document.add(p);
            }
        } catch (NullPointerException e) {
            document.add(new Phrase("Настоящая выписка содержит сведения об индивидуальном предпринимателе:", fLevel[1]));

            PdfPTable idTable = new PdfPTable(1);
            idTable.setSpacingBefore(5);
            idTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            idTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            String surname = docParser.getNodeValue(doc, "//ns1:FNSVipIPResponse/ns1:СвИП/ns1:СвФЛ/ns1:ФИОРус/@Фамилия"),
                    name = docParser.getNodeValue(doc, "//ns1:FNSVipIPResponse/ns1:СвИП/ns1:СвФЛ/ns1:ФИОРус/@Имя"),
                    fathername = docParser.getNodeValue(doc, "//ns1:FNSVipIPResponse/ns1:СвИП/ns1:СвФЛ/ns1:ФИОРус/@Отчество");
            if (!Objects.equals(surname, "")) {
                surname += " ";
            }
            if (!Objects.equals(name, "")) {
                name += " ";
            }
            idTable.addCell(new Phrase(surname + name + fathername, fPlain));
            PdfPCell sign = new PdfPCell(new Phrase("(фамилия, имя, отчество)", fSign));
            sign.setBorder(Rectangle.TOP);
            sign.setHorizontalAlignment(Element.ALIGN_CENTER);
            idTable.addCell(sign);
            idTable.addCell(new Phrase(docParser.getNodeValue(doc, "//ns1:FNSVipIPResponse/ns1:СвИП/@ОГРНИП"), fPlain));
            sign = new PdfPCell(new Phrase("(основной государственный регистрационый номер записи о регистрации индивидуального\n" +
                    "предпринимателя)", fSign));
            sign.setBorder(Rectangle.TOP);
            sign.setHorizontalAlignment(Element.ALIGN_CENTER);
            idTable.addCell(sign);
            document.add(idTable);

            // Call from root node
            creator.parseNode(docParser.getNode(doc, "//ns1:FNSVipIPResponse").item(0), rules, -1);
            document.add(creator.getTable());
        }
    }

    /**
     * Start point for local debugging
     *
     * @param args cmd arguments
     */
    public static void main(String[] args) {
        String pathToXmlFile = "files/egrip.xml";
        String pathToPdfFile = "files/egrip.pdf";
        PdfCreatorEgrip creator = new PdfCreatorEgrip("files/times-new-roman.ttf", null);
        creator.createFullEgrip(new IterParser(namepaces).createXmlDocument(pathToXmlFile), pathToPdfFile);
    }
}

