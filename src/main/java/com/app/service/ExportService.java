package com.app.service;

import com.app.entity.AnnotationTask;
import com.app.entity.CulturalResourceFromUser;
import com.app.entity.User;
import com.app.repository.AnnotationTaskRepository;
import com.app.repository.CulturalResourceFromUserRepository;
import com.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExportService {

    @Autowired
    private CulturalResourceFromUserRepository resourceRepository;

    @Autowired
    private AnnotationTaskRepository annotationTaskRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String EXPORT_DIR = "exports";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 导出单个用户资源为Excel
     */
    @Transactional(readOnly = true)
    public String exportUserResource(Long resourceId, Long userId) throws Exception {
        // 验证资源是否存在且属于该用户
        CulturalResourceFromUser resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new Exception("资源不存在"));

        if (!resource.getUserId().equals(userId)) {
            throw new Exception("无权限访问该资源");
        }

        // 检查标注状态 - 查询该资源对应的标注任务
        List<AnnotationTask> allTasks = annotationTaskRepository.findAll();
        AnnotationTask task = allTasks.stream()
                .filter(t -> t.getResourceId().equals(resourceId) && 
                        "cultural_resources_from_user".equals(t.getResourceSource()))
                .sorted((a, b) -> {
                    // 按创建时间倒序，取最新的任务
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .findFirst()
                .orElse(null);

        if (task == null || (!"已完成".equals(task.getStatus()) && !"AI标注完成".equals(task.getStatus()))) {
            throw new Exception("资源尚未标注完成，无法导出");
        }

        // 获取用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("用户不存在"));

        // 获取标注记录
        List<Map<String, Object>> annotations = getAnnotationRecords(resourceId);

        // 创建输出目录（相对于项目根目录）
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path exportPath = projectRoot.resolve(EXPORT_DIR);
        if (!Files.exists(exportPath)) {
            Files.createDirectories(exportPath);
        }

        // 生成文件名
        String filename = String.format("resource_%d_export_%s.xlsx", 
                resourceId, LocalDateTime.now().format(FILE_DATE_FORMATTER));
        Path filePathObj = exportPath.resolve(filename);
        String filepath = filePathObj.toString();

        // 创建Excel文件
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // 1. 资源信息工作表
            Sheet resourceSheet = workbook.createSheet("资源信息");
            createResourceInfoSheet(resourceSheet, resource, user, headerStyle, dateStyle);

            // 2. 标注详情工作表
            if (!annotations.isEmpty()) {
                Sheet annotationSheet = workbook.createSheet("标注详情");
                createAnnotationSheet(annotationSheet, annotations, headerStyle, dateStyle);
            }

            // 3. 导出信息工作表
            Sheet metadataSheet = workbook.createSheet("导出信息");
            createMetadataSheet(metadataSheet, userId, task.getStatus(), annotations.size(), headerStyle, dateStyle);

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(filepath)) {
                workbook.write(outputStream);
            }
        }

        return filepath;
    }

    /**
     * 批量导出用户资源
     */
    @Transactional(readOnly = true)
    public String batchExportUserResources(Long userId) throws Exception {
        // 获取所有标注任务
        List<AnnotationTask> allTasks = annotationTaskRepository.findAll();
        
        // 获取用户所有标注完成的资源
        List<CulturalResourceFromUser> resources = resourceRepository.findAll().stream()
                .filter(r -> r.getUserId().equals(userId))
                .filter(r -> {
                    // 检查该资源是否有标注完成的任务
                    return allTasks.stream()
                            .filter(t -> t.getResourceId().equals(r.getId()) &&
                                    "cultural_resources_from_user".equals(t.getResourceSource()))
                            .anyMatch(t -> "已完成".equals(t.getStatus()) || "AI标注完成".equals(t.getStatus()));
                })
                .sorted((a, b) -> {
                    if (a.getUploadTime() == null) return 1;
                    if (b.getUploadTime() == null) return -1;
                    return b.getUploadTime().compareTo(a.getUploadTime());
                })
                .toList();

        if (resources.isEmpty()) {
            throw new Exception("没有找到标注完成的资源");
        }

        // 限制最多导出10个资源
        int maxResources = Math.min(resources.size(), 10);
        resources = resources.subList(0, maxResources);

        // 获取用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("用户不存在"));

        // 创建输出目录（相对于项目根目录）
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path exportPath = projectRoot.resolve(EXPORT_DIR);
        if (!Files.exists(exportPath)) {
            Files.createDirectories(exportPath);
        }

        // 生成文件名
        String filename = String.format("user_%d_resources_batch_export_%s.xlsx",
                userId, LocalDateTime.now().format(FILE_DATE_FORMATTER));
        Path filePathObj = exportPath.resolve(filename);
        String filepath = filePathObj.toString();

        // 创建Excel文件
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // 1. 资源列表工作表
            Sheet resourceListSheet = workbook.createSheet("资源列表");
            createResourceListSheet(resourceListSheet, resources, headerStyle, dateStyle);

            // 2. 为每个资源创建单独的工作表（最多10个）
            for (int i = 0; i < Math.min(resources.size(), 10); i++) {
                CulturalResourceFromUser resource = resources.get(i);
                String sheetName = String.format("资源_%d", resource.getId());
                if (sheetName.length() > 31) {
                    sheetName = sheetName.substring(0, 31);
                }
                Sheet resourceSheet = workbook.createSheet(sheetName);
                createResourceInfoSheet(resourceSheet, resource, user, headerStyle, dateStyle);

                // 添加标注详情
                List<Map<String, Object>> annotations = getAnnotationRecords(resource.getId());
                if (!annotations.isEmpty()) {
                    Sheet annotationSheet = workbook.createSheet(sheetName + "_标注");
                    createAnnotationSheet(annotationSheet, annotations, headerStyle, dateStyle);
                }
            }

            // 3. 导出汇总工作表
            Sheet summarySheet = workbook.createSheet("导出汇总");
            createSummarySheet(summarySheet, userId, resources.size(), headerStyle, dateStyle);

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(filepath)) {
                workbook.write(outputStream);
            }
        }

        return filepath;
    }

    /**
     * 获取标注记录
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAnnotationRecords(Long resourceId) {
        String sql = "SELECT ar.id, ar.task_id, ar.annotator_id, ar.entity_name, ar.entity_type, " +
                "ar.description, ar.source, ar.period_era, ar.cultural_region, ar.style_features, " +
                "ar.cultural_value, ar.related_images_url, ar.digital_resource_link, " +
                "at.task_type, at.status as task_status " +
                "FROM annotation_records ar " +
                "JOIN annotation_tasks at ON ar.task_id = at.id " +
                "WHERE at.resource_id = :resourceId " +
                "AND at.resource_source = 'cultural_resources_from_user' " +
                "AND ar.is_latest = 1";
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("resourceId", resourceId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> annotations = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> annotation = new HashMap<>();
            int idx = 0;
            annotation.put("id", row[idx++]);
            annotation.put("task_id", row[idx++]);
            annotation.put("annotator_id", row[idx++]);
            annotation.put("entity_name", row[idx++]);
            annotation.put("entity_type", row[idx++]);
            annotation.put("description", row[idx++]);
            annotation.put("source", row[idx++]);
            annotation.put("period_era", row[idx++]);
            annotation.put("cultural_region", row[idx++]);
            annotation.put("style_features", row[idx++]);
            annotation.put("cultural_value", row[idx++]);
            annotation.put("related_images_url", row[idx++]);
            annotation.put("digital_resource_link", row[idx++]);
            annotation.put("task_type", row[idx++]);
            annotation.put("task_status", row[idx++]);
            annotations.add(annotation);
        }
        
        return annotations;
    }

    /**
     * 创建资源信息工作表
     */
    private void createResourceInfoSheet(Sheet sheet, CulturalResourceFromUser resource, 
                                         User user, CellStyle headerStyle, CellStyle dateStyle) {
        int rowNum = 0;
        
        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"资源ID", "资源标题", "资源类型", "文件格式", "上传时间", 
                            "上传者", "文件路径", "文件大小", "描述", "审核状态"};
        int colNum = 0;
        for (String header : headers) {
            Cell cell = headerRow.createCell(colNum++);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
        }
        
        // 数据行
        Row dataRow = sheet.createRow(rowNum++);
        colNum = 0;
        
        dataRow.createCell(colNum++).setCellValue(resource.getId());
        dataRow.createCell(colNum++).setCellValue(resource.getTitle() != null ? resource.getTitle() : "");
        dataRow.createCell(colNum++).setCellValue(resource.getResourceType() != null ? resource.getResourceType() : "");
        dataRow.createCell(colNum++).setCellValue(resource.getFileFormat() != null ? resource.getFileFormat() : "");
        
        Cell dateCell = dataRow.createCell(colNum++);
        if (resource.getUploadTime() != null) {
            dateCell.setCellValue(resource.getUploadTime().format(DATE_FORMATTER));
            dateCell.setCellStyle(dateStyle);
        } else {
            dateCell.setCellValue("");
        }
        
        dataRow.createCell(colNum++).setCellValue(user.getNickname() != null ? user.getNickname() : String.valueOf(user.getId()));
        dataRow.createCell(colNum++).setCellValue(resource.getStoragePath() != null ? resource.getStoragePath() : "");
        // file_size字段不在表中，使用空值
        dataRow.createCell(colNum++).setCellValue("");
        // description字段不在表中，使用content_feature_data的前100个字符作为描述
        String description = resource.getContentFeatureData() != null && resource.getContentFeatureData().length() > 100 
                ? resource.getContentFeatureData().substring(0, 100) 
                : (resource.getContentFeatureData() != null ? resource.getContentFeatureData() : "");
        dataRow.createCell(colNum++).setCellValue(description);
        dataRow.createCell(colNum++).setCellValue(resource.getAiReviewStatus() != null ? resource.getAiReviewStatus() : "");
        
        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建标注详情工作表
     */
    private void createAnnotationSheet(Sheet sheet, List<Map<String, Object>> annotations, 
                                       CellStyle headerStyle, CellStyle dateStyle) {
        int rowNum = 0;
        
        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"标注任务ID", "标注者ID", "任务类型", "任务状态", "实体名称", 
                            "实体类型", "描述", "来源", "时期年代", "文化区域", 
                            "风格特征", "文化价值", "相关图片链接", "数字资源链接"};
        int colNum = 0;
        for (String header : headers) {
            Cell cell = headerRow.createCell(colNum++);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
        }
        
        // 数据行
        for (Map<String, Object> annotation : annotations) {
            Row dataRow = sheet.createRow(rowNum++);
            colNum = 0;
            
            for (String header : headers) {
                Object value = annotation.get(getKeyFromHeader(header));
                if (value != null) {
                    dataRow.createCell(colNum++).setCellValue(value.toString());
                } else {
                    dataRow.createCell(colNum++).setCellValue("");
                }
            }
        }
        
        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建资源列表工作表
     */
    private void createResourceListSheet(Sheet sheet, List<CulturalResourceFromUser> resources, 
                                        CellStyle headerStyle, CellStyle dateStyle) {
        int rowNum = 0;
        
        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"资源ID", "资源标题", "资源类型", "上传时间", "标注状态"};
        int colNum = 0;
        for (String header : headers) {
            Cell cell = headerRow.createCell(colNum++);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
        }
        
        // 数据行
        for (CulturalResourceFromUser resource : resources) {
            Row dataRow = sheet.createRow(rowNum++);
            colNum = 0;
            
            dataRow.createCell(colNum++).setCellValue(resource.getId());
            dataRow.createCell(colNum++).setCellValue(resource.getTitle() != null ? resource.getTitle() : "");
            dataRow.createCell(colNum++).setCellValue(resource.getResourceType() != null ? resource.getResourceType() : "");
            
            Cell dateCell = dataRow.createCell(colNum++);
            if (resource.getUploadTime() != null) {
                dateCell.setCellValue(resource.getUploadTime().format(DATE_FORMATTER));
                dateCell.setCellStyle(dateStyle);
            } else {
                dateCell.setCellValue("");
            }
            
            // 获取标注状态
            List<AnnotationTask> allTasks = annotationTaskRepository.findAll();
            String status = allTasks.stream()
                    .filter(t -> t.getResourceId().equals(resource.getId()) &&
                            "cultural_resources_from_user".equals(t.getResourceSource()))
                    .sorted((a, b) -> {
                        // 按创建时间倒序，取最新的任务状态
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .map(AnnotationTask::getStatus)
                    .findFirst()
                    .orElse("未知");
            dataRow.createCell(colNum++).setCellValue(status);
        }
        
        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建导出信息工作表
     */
    private void createMetadataSheet(Sheet sheet, Long userId, String status, int annotationCount, 
                                     CellStyle headerStyle, CellStyle dateStyle) {
        int rowNum = 0;
        
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("字段");
        headerCell.setCellStyle(headerStyle);
        
        Cell valueCell = headerRow.createCell(1);
        valueCell.setCellValue("值");
        valueCell.setCellStyle(headerStyle);
        
        // 数据行
        createMetadataRow(sheet, rowNum++, "导出时间", LocalDateTime.now().format(DATE_FORMATTER), dateStyle);
        createMetadataRow(sheet, rowNum++, "导出用户ID", String.valueOf(userId), null);
        createMetadataRow(sheet, rowNum++, "资源状态", status, null);
        createMetadataRow(sheet, rowNum++, "标注记录数量", String.valueOf(annotationCount), null);
        
        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * 创建汇总工作表
     */
    private void createSummarySheet(Sheet sheet, Long userId, int resourceCount, 
                                   CellStyle headerStyle, CellStyle dateStyle) {
        int rowNum = 0;
        
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("字段");
        headerCell.setCellStyle(headerStyle);
        
        Cell valueCell = headerRow.createCell(1);
        valueCell.setCellValue("值");
        valueCell.setCellStyle(headerStyle);
        
        // 数据行
        createMetadataRow(sheet, rowNum++, "导出时间", LocalDateTime.now().format(DATE_FORMATTER), dateStyle);
        createMetadataRow(sheet, rowNum++, "导出用户ID", String.valueOf(userId), null);
        createMetadataRow(sheet, rowNum++, "总资源数量", String.valueOf(resourceCount), null);
        createMetadataRow(sheet, rowNum++, "导出限制", "最多导出10个资源以控制文件大小", null);
        
        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * 创建元数据行
     */
    private void createMetadataRow(Sheet sheet, int rowNum, String label, String value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        if (style != null) {
            valueCell.setCellStyle(style);
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建日期样式
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }

    /**
     * 从表头获取键名
     */
    private String getKeyFromHeader(String header) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("标注任务ID", "task_id");
        mapping.put("标注者ID", "annotator_id");
        mapping.put("任务类型", "task_type");
        mapping.put("任务状态", "task_status");
        mapping.put("实体名称", "entity_name");
        mapping.put("实体类型", "entity_type");
        mapping.put("描述", "description");
        mapping.put("来源", "source");
        mapping.put("时期年代", "period_era");
        mapping.put("文化区域", "cultural_region");
        mapping.put("风格特征", "style_features");
        mapping.put("文化价值", "cultural_value");
        mapping.put("相关图片链接", "related_images_url");
        mapping.put("数字资源链接", "digital_resource_link");
        return mapping.getOrDefault(header, header.toLowerCase().replace(" ", "_"));
    }
}
