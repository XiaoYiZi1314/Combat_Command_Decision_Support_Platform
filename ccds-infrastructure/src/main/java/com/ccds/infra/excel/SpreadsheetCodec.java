package com.ccds.infra.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Component;

/**
 * 解析与写出花名册表格。兼容现网 xlsx / html-xls / csv，不引入第三方库。
 *
 * @author ccds
 * @since 0.1.0
 */
@Component
public class SpreadsheetCodec {

    private static final Pattern SHARED_ITEM = Pattern.compile("<si[\\s>][\\s\\S]*?</si>", Pattern.CASE_INSENSITIVE);

    private static final Pattern TEXT_ITEM = Pattern.compile("<t[^>]*>([\\s\\S]*?)</t>", Pattern.CASE_INSENSITIVE);

    private static final Pattern ROW_ITEM = Pattern.compile("<row\\b[^>]*>([\\s\\S]*?)</row>", Pattern.CASE_INSENSITIVE);

    private static final Pattern CELL_ITEM = Pattern.compile("<c\\b([^>]*)>([\\s\\S]*?)</c>|<c\\b([^>]*)/>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CELL_REF = Pattern.compile("r=\"([A-Z]+)(\\d+)\"", Pattern.CASE_INSENSITIVE);

    private static final Pattern CELL_TYPE = Pattern.compile("t=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private static final Pattern CELL_VALUE = Pattern.compile("<v[^>]*>([\\s\\S]*?)</v>", Pattern.CASE_INSENSITIVE);

    private static final Pattern HTML_ROW = Pattern.compile("<tr\\b[^>]*>([\\s\\S]*?)</tr>", Pattern.CASE_INSENSITIVE);

    private static final Pattern HTML_CELL = Pattern.compile("<t[dh]\\b[^>]*>([\\s\\S]*?)</t[dh]>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private static final String XLSX_CONTENT_TYPES = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
            <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            </Types>
            """;

    private static final String XLSX_ROOT_RELS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
            """;

    private static final String XLSX_WB_RELS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
            <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
            </Relationships>
            """;

    private static final String XLSX_STYLES = """
            <?xml version="1.0" encoding="UTF-8"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
            <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
            <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
            <cellXfs count="1"><xf numFmtId="49" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs>
            </styleSheet>
            """;

    /**
     * 按文件名与内容解析表格。
     *
     * @param fileName 文件名，允许为 null
     * @param bytes    文件字节
     * @return 表格
     */
    public SpreadsheetTable parse(String fileName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return SpreadsheetTable.builder().rows(new ArrayList<List<String>>()).build();
        }
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || isZip(bytes)) {
            return parseXlsx(bytes);
        }
        String text = decodeText(bytes);
        if (name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".xls")
                || text.contains("<table") || text.contains("<Workbook")) {
            return parseHtml(text);
        }
        return parseCsv(text);
    }

    /**
     * 写出 xlsx。
     *
     * @param sheetName 工作表名
     * @param table     表格
     * @return xlsx 字节
     */
    public byte[] writeXlsx(String sheetName, SpreadsheetTable table) {
        List<List<String>> rows = table == null || table.getRows() == null
                ? List.of()
                : table.getRows();
        List<String> shared = new ArrayList<String>();
        Map<String, Integer> index = new LinkedHashMap<String, Integer>();
        StringBuilder sheet = new StringBuilder();
        sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sheet.append("<sheetData>");
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            sheet.append("<row r=\"").append(r + 1).append("\">");
            if (row != null) {
                for (int c = 0; c < row.size(); c++) {
                    String value = row.get(c) == null ? "" : row.get(c);
                    Integer si = index.get(value);
                    if (si == null) {
                        si = shared.size();
                        shared.add(value);
                        index.put(value, si);
                    }
                    sheet.append("<c r=\"").append(columnName(c)).append(r + 1)
                            .append("\" t=\"s\"><v>").append(si).append("</v></c>");
                }
            }
            sheet.append("</row>");
        }
        sheet.append("</sheetData></worksheet>");
        String safeName = sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName;
        String workbook = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"" + xmlEscape(safeName)
                + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
        StringBuilder sharedXml = new StringBuilder();
        sharedXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sharedXml.append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"")
                .append(shared.size()).append("\" uniqueCount=\"").append(shared.size()).append("\">");
        for (String item : shared) {
            sharedXml.append("<si><t xml:space=\"preserve\">").append(xmlEscape(item)).append("</t></si>");
        }
        sharedXml.append("</sst>");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out)) {
            put(zip, "[Content_Types].xml", XLSX_CONTENT_TYPES);
            put(zip, "_rels/.rels", XLSX_ROOT_RELS);
            put(zip, "xl/_rels/workbook.xml.rels", XLSX_WB_RELS);
            put(zip, "xl/workbook.xml", workbook);
            put(zip, "xl/styles.xml", XLSX_STYLES);
            put(zip, "xl/sharedStrings.xml", sharedXml.toString());
            put(zip, "xl/worksheets/sheet1.xml", sheet.toString());
            zip.finish();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("xlsx write failed", ex);
        }
    }

    private SpreadsheetTable parseXlsx(byte[] bytes) {
        Map<String, String> parts = new LinkedHashMap<String, String>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    parts.put(entry.getName().replace('\\', '/'), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("xlsx parse failed", ex);
        }
        List<String> shared = parseSharedStrings(firstPart(parts, "xl/sharedStrings.xml"));
        String sheetXml = firstSheet(parts);
        List<List<String>> rows = new ArrayList<List<String>>();
        if (sheetXml == null) {
            return SpreadsheetTable.builder().rows(rows).build();
        }
        Matcher rowMatcher = ROW_ITEM.matcher(sheetXml);
        while (rowMatcher.find()) {
            List<String> row = new ArrayList<String>();
            Matcher cellMatcher = CELL_ITEM.matcher(rowMatcher.group(1) == null ? "" : rowMatcher.group(1));
            while (cellMatcher.find()) {
                String attrs = cellMatcher.group(1) != null ? cellMatcher.group(1) : cellMatcher.group(3);
                String body = cellMatcher.group(2) == null ? "" : cellMatcher.group(2);
                int col = columnIndex(attrs);
                while (row.size() < col) {
                    row.add("");
                }
                row.add(readCell(attrs, body, shared));
            }
            rows.add(row);
        }
        return SpreadsheetTable.builder().rows(rows).build();
    }

    private List<String> parseSharedStrings(String xml) {
        List<String> shared = new ArrayList<String>();
        if (xml == null || xml.isBlank()) {
            return shared;
        }
        Matcher itemMatcher = SHARED_ITEM.matcher(xml);
        while (itemMatcher.find()) {
            StringBuilder text = new StringBuilder();
            Matcher textMatcher = TEXT_ITEM.matcher(itemMatcher.group());
            while (textMatcher.find()) {
                text.append(xmlUnescape(textMatcher.group(1)));
            }
            shared.add(text.toString());
        }
        return shared;
    }

    private String readCell(String attrs, String body, List<String> shared) {
        String type = matchFirst(CELL_TYPE, attrs == null ? "" : attrs);
        Matcher valueMatcher = CELL_VALUE.matcher(body == null ? "" : body);
        String raw = valueMatcher.find() ? xmlUnescape(valueMatcher.group(1)) : "";
        if ("s".equals(type) && !raw.isBlank()) {
            int idx = parseIntSafe(raw);
            if (idx >= 0 && idx < shared.size()) {
                return shared.get(idx);
            }
        }
        if ("inlineStr".equals(type)) {
            Matcher textMatcher = TEXT_ITEM.matcher(body == null ? "" : body);
            StringBuilder text = new StringBuilder();
            while (textMatcher.find()) {
                text.append(xmlUnescape(textMatcher.group(1)));
            }
            return text.toString();
        }
        return raw;
    }

    private int columnIndex(String attrs) {
        Matcher matcher = CELL_REF.matcher(attrs == null ? "" : attrs);
        if (!matcher.find()) {
            return 0;
        }
        return columnToIndex(matcher.group(1));
    }

    private SpreadsheetTable parseHtml(String text) {
        List<List<String>> rows = new ArrayList<List<String>>();
        Matcher rowMatcher = HTML_ROW.matcher(text);
        while (rowMatcher.find()) {
            List<String> row = new ArrayList<String>();
            Matcher cellMatcher = HTML_CELL.matcher(rowMatcher.group(1));
            while (cellMatcher.find()) {
                String cell = HTML_TAG.matcher(cellMatcher.group(1)).replaceAll("");
                row.add(htmlUnescape(cell).trim());
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        return SpreadsheetTable.builder().rows(rows).build();
    }

    private SpreadsheetTable parseCsv(String text) {
        List<List<String>> rows = new ArrayList<List<String>>();
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        List<String> current = new ArrayList<String>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < normalized.length() && normalized.charAt(i + 1) == '"') {
                        cell.append('"');
                        i += 1;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(ch);
                }
                continue;
            }
            if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                current.add(cell.toString().trim());
                cell.setLength(0);
            } else if (ch == '\n') {
                current.add(cell.toString().trim());
                cell.setLength(0);
                if (!isEmptyRow(current)) {
                    rows.add(current);
                }
                current = new ArrayList<String>();
            } else {
                cell.append(ch);
            }
        }
        current.add(cell.toString().trim());
        if (!isEmptyRow(current)) {
            rows.add(current);
        }
        return SpreadsheetTable.builder().rows(rows).build();
    }

    private boolean isEmptyRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String cell : row) {
            if (cell != null && !cell.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String decodeText(byte[] bytes) {
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return new String(bytes, Charset.forName("UTF-16LE"));
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private boolean isZip(byte[] bytes) {
        return bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    private String firstPart(Map<String, String> parts, String name) {
        for (Map.Entry<String, String> entry : parts.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String firstSheet(Map<String, String> parts) {
        for (Map.Entry<String, String> entry : parts.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            if (key.startsWith("xl/worksheets/sheet") && key.endsWith(".xml")) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String columnName(int index) {
        StringBuilder builder = new StringBuilder();
        int current = index;
        while (current >= 0) {
            builder.insert(0, (char) ('A' + (current % 26)));
            current = current / 26 - 1;
        }
        return builder.toString();
    }

    private int columnToIndex(String name) {
        int result = 0;
        String col = name == null ? "" : name.toUpperCase(Locale.ROOT);
        for (int i = 0; i < col.length(); i++) {
            result = result * 26 + (col.charAt(i) - 'A' + 1);
        }
        return Math.max(result - 1, 0);
    }

    private String matchFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private int parseIntSafe(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String xmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String xmlUnescape(String value) {
        return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
    }

    private String htmlUnescape(String value) {
        return xmlUnescape(value).replace("&nbsp;", " ");
    }
}
