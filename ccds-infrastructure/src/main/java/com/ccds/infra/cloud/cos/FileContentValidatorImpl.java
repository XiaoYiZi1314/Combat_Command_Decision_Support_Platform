package com.ccds.infra.cloud.cos;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Component;

/**
 * 基于文件签名和容器结构校验上传内容，不信任客户端声明的 MIME。
 *
 * @author ccds
 * @since 0.1.0
 */
@Component
public class FileContentValidatorImpl implements FileContentValidator {

    private static final String PDF = "application/pdf";
    private static final String JPEG = "image/jpeg";
    private static final String PNG = "image/png";
    private static final String DOC = "application/msword";
    private static final String DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String TEXT = "text/plain";

    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] DOC_SIGNATURE = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final byte[] DOC_WORD_STREAM_NAME = {
            'W', 0, 'o', 0, 'r', 0, 'd', 0, 'D', 0, 'o', 0,
            'c', 0, 'u', 0, 'm', 0, 'e', 0, 'n', 0, 't', 0
    };
    private static final int MIN_JPEG_LENGTH = 4;
    private static final int MAX_ZIP_ENTRIES = 10_000;
    private static final int MAX_TEXT_CONTROL_PERCENT = 2;
    private static final String DOCX_CONTENT_TYPES = "[Content_Types].xml";
    private static final String DOCX_DOCUMENT = "word/document.xml";

    /** {@inheritDoc} */
    @Override
    public boolean matches(String contentType, byte[] content) {
        if (contentType == null || content == null || content.length == 0) {
            return false;
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case PDF -> startsWith(content, PDF_SIGNATURE);
            case JPEG -> isJpeg(content);
            case PNG -> startsWith(content, PNG_SIGNATURE);
            case DOC -> startsWith(content, DOC_SIGNATURE)
                    && contains(content, DOC_WORD_STREAM_NAME);
            case DOCX -> isDocx(content);
            case TEXT -> isUtf8Text(content);
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean contains(byte[] content, byte[] expected) {
        if (content.length < expected.length) {
            return false;
        }
        for (int start = 0; start <= content.length - expected.length; start++) {
            boolean matched = true;
            for (int offset = 0; offset < expected.length; offset++) {
                if (content[start + offset] != expected[offset]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private boolean isJpeg(byte[] content) {
        return content.length >= MIN_JPEG_LENGTH
                && content[0] == (byte) 0xFF
                && content[1] == (byte) 0xD8
                && content[content.length - 2] == (byte) 0xFF
                && content[content.length - 1] == (byte) 0xD9;
    }

    private boolean isDocx(byte[] content) {
        Set<String> requiredEntries = new HashSet<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            int entryCount = 0;
            while ((entry = input.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ZIP_ENTRIES) {
                    return false;
                }
                String name = entry.getName();
                if (DOCX_CONTENT_TYPES.equals(name) || DOCX_DOCUMENT.equals(name)) {
                    requiredEntries.add(name);
                }
            }
            return requiredEntries.contains(DOCX_CONTENT_TYPES)
                    && requiredEntries.contains(DOCX_DOCUMENT);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isUtf8Text(byte[] content) {
        int controlCount = 0;
        for (byte value : content) {
            int unsigned = Byte.toUnsignedInt(value);
            if (unsigned == 0) {
                return false;
            }
            if (unsigned < 0x20 && unsigned != '\t' && unsigned != '\n' && unsigned != '\r') {
                controlCount++;
            }
        }
        if (controlCount * 100L > (long) content.length * MAX_TEXT_CONTROL_PERCENT) {
            return false;
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
            return true;
        } catch (CharacterCodingException ex) {
            return false;
        }
    }
}
