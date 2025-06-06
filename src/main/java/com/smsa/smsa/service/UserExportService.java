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
import java.nio.charset.StandardCharsets;
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

        if (users.isEmpty()) {
            System.out.println("No users found in the database.");
            return null;
        }

        // Estimate how many users fit in ~4KB
        int estimatedRowSize = estimateRowSize(users.get(0)) + 500; // buffer for metadata
        int usersPerFile = Math.max(1, (int)(4096 * 0.9 / estimatedRowSize)); // 90% of 4KB safety margin
        int totalFiles = (int) Math.ceil(users.size() / (double) usersPerFile);

        System.out.println("Estimated row size (with buffer): " + estimatedRowSize + " bytes");
        System.out.println("Users per file (approx.): " + usersPerFile);
        System.out.println("Total users: " + users.size());
        System.out.println("Total files to generate: " + totalFiles);

        // Temp directory for XLSX files
        File tempDir = new File(folderPath, "temp_xls");
        if (!tempDir.exists()) tempDir.mkdirs();

        int fileCount = 1;
        for (int i = 0; i < users.size(); i += usersPerFile) {
            List<User> chunk = users.subList(i, Math.min(i + usersPerFile, users.size()));
            System.out.println("Creating file users_part_" + fileCount + ".xlsx with users from " + i + " to " + (i + chunk.size() - 1));

            Workbook workbook = createWorkbookWithHeaders();
            Sheet sheet = workbook.getSheetAt(0);

            int rowNum = 1;
            for (User user : chunk) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getUsername());
                row.createCell(2).setCellValue(user.getEmail());
                row.createCell(3).setCellValue(user.getPassword());
                row.createCell(4).setCellValue(user.getCreatedAt().toString());
                row.createCell(5).setCellValue(user.getUpdatedAt().toString());
            }

            for (int col = 0; col < 6; col++) sheet.autoSizeColumn(col);

            File file = new File(tempDir, "users_part_" + fileCount++ + ".xlsx");
            saveWorkbook(workbook, file);
            workbook.close();
        }

        // Zip all files
        String zipFilePath = folderPath + "/users_export.zip";
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            File[] xlsFiles = tempDir.listFiles((dir, name) -> name.endsWith(".xlsx"));
            if (xlsFiles != null) {
                for (File file : xlsFiles) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        zos.putNextEntry(new ZipEntry(file.getName()));
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                        System.out.println("Added to ZIP: " + file.getName());
                    }
                }
            }
        }

        // Cleanup
        File[] tempFiles = tempDir.listFiles();
        if (tempFiles != null) {
            for (File file : tempFiles) file.delete();
        }
        tempDir.delete();

        System.out.println("ZIP export complete: " + zipFilePath);
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

    private int estimateRowSize(User user) {
        String data = user.getId() +
                user.getUsername() +
                user.getEmail() +
                user.getPassword() +
                user.getCreatedAt().toString() +
                user.getUpdatedAt().toString();
        int size = data.getBytes(StandardCharsets.UTF_8).length;
        System.out.println("Estimated raw row size for user " + user.getUsername() + ": " + size + " bytes");
        return size;
    }
}

