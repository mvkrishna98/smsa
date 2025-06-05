/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smsa.smsa.service;

import com.smsa.smsa.entity.User;
import com.smsa.smsa.repository.UserRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserExportService {

    @Autowired
    private UserRepository userRepository;

    public String exportUsersToZip(String folderPath) throws IOException {
        List<User> users = userRepository.findAll();

        final int MAX_ROWS_PER_FILE = 50; // Adjust based on approx file size

        // Create temp directory to save multiple XLSX files
        File tempDir = new File(folderPath, "temp_xls");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        int fileCount = 0;
        int startIndex = 0;

        while (startIndex < users.size()) {
            int endIndex = Math.min(startIndex + MAX_ROWS_PER_FILE, users.size());
            List<User> subList = users.subList(startIndex, endIndex);

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Users");

            // Header rowww
            Row header = sheet.createRow(0);
            String[] headers = {"ID", "Username", "Email", "Password", "Created At", "Updated At"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // Data rows
            int rowNum = 1;
            for (User user : subList) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getUsername());
                row.createCell(2).setCellValue(user.getEmail());
                row.createCell(3).setCellValue(user.getPassword());
                row.createCell(4).setCellValue(user.getCreatedAt().toString());
                row.createCell(5).setCellValue(user.getUpdatedAt().toString());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            String fileName = "users_part_" + (++fileCount) + ".xlsx";
            File xlsFile = new File(tempDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(xlsFile)) {
                workbook.write(fos);
            }
            workbook.close();

            startIndex = endIndex;
        }

        // Create ZIP file containing all XLSX files
        String zipFilePath = folderPath + "/users_export.zip";
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            File[] xlsFiles = tempDir.listFiles((dir, name) -> name.endsWith(".xlsx"));
            if (xlsFiles != null) {
                for (File file : xlsFiles) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry zipEntry = new ZipEntry(file.getName());
                        zos.putNextEntry(zipEntry);

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                    }
                }
            }
        }

        // Cleanup temporary XLS files and folder
        File[] tempFiles = tempDir.listFiles();
        if (tempFiles != null) {
            for (File file : tempFiles) {
                file.delete();
            }
        }
        tempDir.delete();

        return zipFilePath;
    }
}

