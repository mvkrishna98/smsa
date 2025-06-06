/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smsa.smsa.Controller;

import com.smsa.smsa.entity.User;
import com.smsa.smsa.service.UserExportCsvService;
import com.smsa.smsa.service.UserExportService;
import com.smsa.smsa.service.UserService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author dell
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User updated = userService.updateUser(id, user);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Autowired
    private UserExportService exportService;

    @GetMapping("/export/xls")
    public ResponseEntity<InputStreamResource> exportUsersToExcel() {
        try {
            String path = System.getProperty("java.io.tmpdir"); // safer across OS
            String filePath = exportService.exportUsersToZip(path);
            File zipFile = new File(filePath);

            if (!zipFile.exists()) {
                return ResponseEntity.status(404).build();
            }

            InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users_export.zip")
                    .contentLength(zipFile.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @Autowired
    private UserExportCsvService exportCsvService;

    @GetMapping("/export/csv")
    public ResponseEntity<InputStreamResource> exportUsersToCsvZip() {
        try {
            String path = System.getProperty("java.io.tmpdir"); // system temp folder
            String filePath = exportCsvService.exportUsersToZipCsv(path);
            if (filePath == null) {
                return ResponseEntity.status(404).build();
            }
            File zipFile = new File(filePath);
            if (!zipFile.exists()) {
                return ResponseEntity.status(404).build();
            }

            InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users_export_csv.zip")
                    .contentLength(zipFile.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

}
