package com.ccds.infra.cloud.cos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

/**
 * 无新增依赖的文件正文抽取器，支持 TXT 与 DOCX。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Component
public class FileTextExtractorImpl implements FileTextExtractor {

    private static final String TXT = "text/plain";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String DOCUMENT_XML = "word/document.xml";
    private static final int MAX_TEXT_LENGTH = 200_000;

    /** {@inheritDoc} */
    @Override
    public String extract(String contentType, byte[] content) {
        if (content == null || content.length == 0 || contentType == null) {
            return null;
        }
        if (TXT.equalsIgnoreCase(contentType)) {
            return limit(new String(content, StandardCharsets.UTF_8));
        }
        if (DOCX.equalsIgnoreCase(contentType)) {
            return extractDocx(content);
        }
        return null;
    }

    private String extractDocx(byte[] content) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (DOCUMENT_XML.equals(entry.getName())) {
                    byte[] xml = readEntry(zip);
                    return limit(extractXmlText(xml));
                }
            }
        } catch (Exception ex) {
            log.warn("DOCX正文抽取失败");
        }
        return null;
    }

    private byte[] readEntry(ZipInputStream zip) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        while ((read = zip.read(buffer)) >= 0) {
            total += read;
            if (total > MAX_TEXT_LENGTH * 4) {
                return new byte[0];
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private String extractXmlText(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        NodeList texts = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml)).getElementsByTagNameNS("*", "t");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < texts.getLength(); i++) {
            Node node = texts.item(i);
            result.append(node.getTextContent());
        }
        return result.toString();
    }

    private String limit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
    }
}
