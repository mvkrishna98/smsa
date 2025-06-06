package com.smsa.smsa.service;

import com.smsa.smsa.entity.User;
import com.smsa.smsa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class UserExportCsvService {

    @Autowired
    private UserRepository userRepository;

    public String exportUsersToZipCsv(String folderPath) throws IOException {
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            System.out.println("No users found in the database.");
            return null;
        }

        // Estimate row size + buffer, similar to XLSX logic
        int estimatedRowSize = estimateRowSize(users.get(0)) + 100; // CSV smaller than XLSX
        int usersPerFile = Math.max(1, (int)(4096 * 0.9 / estimatedRowSize)); // 90% of 4KB
        int totalFiles = (int) Math.ceil(users.size() / (double) usersPerFile);

        System.out.println("Estimated CSV row size (with buffer): " + estimatedRowSize + " bytes");
        System.out.println("Users per CSV file (approx.): " + usersPerFile);
        System.out.println("Total users: " + users.size());
        System.out.println("Total CSV files to generate: " + totalFiles);

        // Create temp directory for CSV files
        File tempDir = new File(folderPath, "temp_csv");
        if (!tempDir.exists()) tempDir.mkdirs();

        int fileCount = 1;
        for (int i = 0; i < users.size(); i += usersPerFile) {
            List<User> chunk = users.subList(i, Math.min(i + usersPerFile, users.size()));

            File csvFile = new File(tempDir, "users_part_" + fileCount++ + ".csv");

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(csvFile), StandardCharsets.UTF_8))) {

                // Write CSV header
                writer.write("ID,Username,Email,Password,Created At,Updated At");
                writer.newLine();

                // Write user data rows
                for (User user : chunk) {
                    writer.write(csvEscape(user.getId().toString()) + ","
                            + csvEscape(user.getUsername()) + ","
                            + csvEscape(user.getEmail()) + ","
                            + csvEscape(user.getPassword()) + ","
                            + csvEscape(user.getCreatedAt().toString()) + ","
                            + csvEscape(user.getUpdatedAt().toString()));
                    writer.newLine();
                }
            }
            System.out.println("Created CSV file: " + csvFile.getName());
        }

        // Zip CSV files
        String zipFilePath = folderPath + "/users_export_csv.zip";
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            File[] csvFiles = tempDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (csvFiles != null) {
                for (File file : csvFiles) {
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

        // Cleanup temp files
        File[] tempFiles = tempDir.listFiles();
        if (tempFiles != null) {
            for (File file : tempFiles) file.delete();
        }
        tempDir.delete();

        System.out.println("CSV ZIP export complete: " + zipFilePath);
        return zipFilePath;
    }

    // Helper method to estimate approx CSV row size in bytes
    private int estimateRowSize(User user) {
        String data = user.getId() + "," +
                user.getUsername() + "," +
                user.getEmail() + "," +
                user.getPassword() + "," +
                user.getCreatedAt().toString() + "," +
                user.getUpdatedAt().toString();
        int size = data.getBytes(StandardCharsets.UTF_8).length;
        System.out.println("Estimated raw CSV row size for user " + user.getUsername() + ": " + size + " bytes");
        return size;
    }

    // CSV escape method (for commas, quotes, newlines)
    private String csvEscape(String input) {
        if (input == null) return "";

        String escaped = input.replace("\"", "\"\"");

        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
