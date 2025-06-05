/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smsa.smsa.service;

import com.smsa.smsa.entity.User;
import com.smsa.smsa.repository.UserRepository;
import java.io.ByteArrayOutputStream;
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

        // Create temp folder
        File tempDir = new File(folderPath, "temp_xls");
        if (!tempDir.exists()) tempDir.mkdirs();

        int fileCount = 1;
        Workbook workbook = createWorkbookWithHeaders();
        Sheet sheet = workbook.getSheetAt(0);
        int rowNum = 1;

        for (User user : users) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getUsername());
            row.createCell(2).setCellValue(user.getEmail());
            row.createCell(3).setCellValue(user.getPassword());
            row.createCell(4).setCellValue(user.getCreatedAt().toString());
            row.createCell(5).setCellValue(user.getUpdatedAt().toString());

            // Estimate size after each row
            ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
            workbook.write(tempOut);
            int currentSize = tempOut.toByteArray().length;
            tempOut.close();

            if (currentSize >= 4096) {
                saveWorkbook(workbook, new File(tempDir, "users_part_" + fileCount + ".xlsx"));
                fileCount++;
                workbook.close();
                workbook = createWorkbookWithHeaders();
                sheet = workbook.getSheetAt(0);
                rowNum = 1;
            }
        }

        // Save last workbook if it has remaining rows
        if (rowNum > 1) {
            saveWorkbook(workbook, new File(tempDir, "users_part_" + fileCount + ".xlsx"));
            workbook.close();
        }

        // Create ZIP file
        String zipFilePath = folderPath + "/users_export.zip";
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            File[] files = tempDir.listFiles((dir, name) -> name.endsWith(".xlsx"));
            if (files != null) {
                for (File file : files) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry entry = new ZipEntry(file.getName());
                        zos.putNextEntry(entry);
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    }
                }
            }
        }

        // Cleanup
        File[] tempFiles = tempDir.listFiles();
        if (tempFiles != null) {
            for (File f : tempFiles) f.delete();
        }
        tempDir.delete();

        return zipFilePath;
    }

    private Workbook createWorkbookWithHeaders() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Users");
        String[] headers = {"ID", "Username", "Email", "Password", "Created At", "Updated At"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        return workbook;
    }

    private void saveWorkbook(Workbook workbook, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
    }
}

