package com.ronyxdumb.rassegnascuola;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DocxExporter {
    private static final String CONTENT_TYPES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>";
    private static final String ROOT_RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>";
    private DocxExporter() {}

    public static byte[] build(List<NewsEngine.Item> items, List<String> notes, Sector sector) throws Exception {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");
        paragraph(xml, "RASSEGNA SCUOLA · " + sector.label.toUpperCase(Locale.ITALY), true);
        paragraph(xml, "by Francesco Pio Pipino", false);
        paragraph(xml, sector.label + " — " + romeDate("dd/MM/yyyy", System.currentTimeMillis()), false);
        paragraph(xml, "", false);
        int index = 1;
        for (NewsEngine.Item item : items) {
            paragraph(xml, index++ + ". " + item.title, true);
            String published = item.published == 0 ? "" : "   |   Pubblicata: " + romeDate("dd/MM/yyyy HH:mm", item.published);
            paragraph(xml, "Fonte: " + item.source + "   |   Ambito: " + item.scope + published, false);
            paragraph(xml, "Riassunto: " + item.summary, false);
            paragraph(xml, "Link: " + item.url, false);
            if (!item.signals.isEmpty()) paragraph(xml, "Indicazioni: " + String.join("; ", item.signals), false);
            paragraph(xml, "", false);
        }
        paragraph(xml, "Nota sul metodo", true);
        paragraph(xml, "Gli estratti provengono dai feed degli editori. La data indica la pubblicazione, non necessariamente il giorno dell’evento.", false);
        paragraph(xml, "Esito delle fonti", true);
        for (String note : notes) paragraph(xml, "• " + note, false);
        xml.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1134\" w:right=\"1247\" w:bottom=\"1134\" w:left=\"1247\"/></w:sectPr></w:body></w:document>");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(buffer)) {
            entry(out, "[Content_Types].xml", CONTENT_TYPES);
            entry(out, "_rels/.rels", ROOT_RELS);
            entry(out, "word/document.xml", xml.toString());
        }
        return buffer.toByteArray();
    }
    private static String romeDate(String pattern, long time) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.ITALY);
        f.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Rome"));
        return f.format(new Date(time));
    }
    private static void paragraph(StringBuilder xml, String text, boolean heading) {
        xml.append("<w:p>");
        if (heading) xml.append("<w:pPr><w:spacing w:before=\"240\" w:after=\"100\"/></w:pPr>");
        xml.append("<w:r>");
        if (heading) xml.append("<w:rPr><w:b/><w:sz w:val=\"28\"/></w:rPr>");
        xml.append("<w:t xml:space=\"preserve\">").append(escape(text)).append("</w:t></w:r></w:p>");
    }
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
    private static void entry(ZipOutputStream out, String name, String text) throws Exception {
        out.putNextEntry(new ZipEntry(name));
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
}

