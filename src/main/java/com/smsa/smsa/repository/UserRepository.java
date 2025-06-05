/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smsa.smsa.repository;

import com.smsa.smsa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author dell
 */



public interface UserRepository extends JpaRepository<User, Long> {
    // You can add custom query methods here like:
    User findByUsername(String username);
    User findByEmail(String email);
}
