package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseAttachmentVO;
import com.finex.auth.service.ExpenseAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseAttachmentServiceImpl implements ExpenseAttachmentService {

    private static final String DEFAULT_FILE_NAME = "attachment";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final ObjectMapper objectMapper;

    @Value("${finex.expense.attachments.storage-path:${user.dir}/storage/expense-attachments}")
    private String storagePath;

    @Override
    public ExpenseAttachmentVO uploadAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请先选择附件");
        }

        String attachmentId = UUID.randomUUID().toString().replace("-", "");
        String fileName = sanitizeFileName(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType(), fileName);
        Path root = ensureStorageRoot();
        Path binaryPath = root.resolve(attachmentId + ".bin");
        Path metadataPath = root.resolve(attachmentId + ".json");

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, binaryPath, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("attachmentId", attachmentId);
            metadata.put("fileName", fileName);
            metadata.put("contentType", contentType);
            metadata.put("fileSize", file.getSize());
            metadata.put("previewUrl", buildPreviewUrl(attachmentId));

            Files.writeString(metadataPath, objectMapper.writeValueAsString(metadata));

            ExpenseAttachmentVO attachment = new ExpenseAttachmentVO();
            attachment.setAttachmentId(attachmentId);
            attachment.setFileName(fileName);
            attachment.setContentType(contentType);
            attachment.setFileSize(file.getSize());
            attachment.setPreviewUrl(buildPreviewUrl(attachmentId));
            return attachment;
        } catch (Exception ex) {
            deleteIfExists(binaryPath);
            deleteIfExists(metadataPath);
            throw new IllegalStateException("附件上传失败，请稍后重试", ex);
        }
    }

    @Override
    public StoredExpenseAttachment loadAttachment(String attachmentId) {
        String normalizedId = normalizeAttachmentId(attachmentId);
        Path root = ensureStorageRoot();
        Path binaryPath = root.resolve(normalizedId + ".bin");
        Path metadataPath = root.resolve(normalizedId + ".json");
        if (!Files.exists(binaryPath) || !Files.exists(metadataPath)) {
            throw new IllegalArgumentException("附件不存在或已失效");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = objectMapper.readValue(Files.readString(metadataPath), LinkedHashMap.class);
            String fileName = sanitizeFileName(String.valueOf(metadata.getOrDefault("fileName", DEFAULT_FILE_NAME)));
            String contentType = normalizeContentType(
                    metadata.get("contentType") == null ? null : String.valueOf(metadata.get("contentType")),
                    fileName
            );
            Resource resource = new FileSystemResource(binaryPath);
            return new StoredExpenseAttachment(resource, fileName, contentType, Files.size(binaryPath));
        } catch (IOException ex) {
            throw new IllegalStateException("附件读取失败，请稍后重试", ex);
        }
    }

    private Path ensureStorageRoot() {
        try {
            Path root = Path.of(storagePath).toAbsolutePath().normalize();
            Files.createDirectories(root);
            return root;
        } catch (IOException ex) {
            throw new IllegalStateException("附件目录初始化失败", ex);
        }
    }

    private void deleteIfExists(Path path) {
        try {
            if (path != null) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Ignore cleanup failure and keep the original exception.
        }
    }

    private String buildPreviewUrl(String attachmentId) {
        return "/api/auth/expenses/attachments/" + attachmentId + "/content";
    }

    private String sanitizeFileName(String originalFileName) {
        String normalized = originalFileName == null ? "" : originalFileName.trim();
        if (normalized.isEmpty()) {
            return DEFAULT_FILE_NAME;
        }
        normalized = normalized.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0) {
            normalized = normalized.substring(lastSlash + 1);
        }
        normalized = normalized.replaceAll("[\\r\\n]", "");
        return normalized.isBlank() ? DEFAULT_FILE_NAME : normalized;
    }

    private String normalizeAttachmentId(String attachmentId) {
        String normalized = attachmentId == null ? "" : attachmentId.trim();
        if (!normalized.matches("[A-Za-z0-9]{16,64}")) {
            throw new IllegalArgumentException("附件标识无效");
        }
        return normalized;
    }

    private String normalizeContentType(String rawContentType, String fileName) {
        String normalized = rawContentType == null ? "" : rawContentType.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerName.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        return DEFAULT_CONTENT_TYPE;
    }
}
