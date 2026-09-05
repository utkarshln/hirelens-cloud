package com.hirelens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HireLensApplication {
    public static void main(String[] args) {
        SpringApplication.run(HireLensApplication.class, args);
        System.out.println("""
            ╔══════════════════════════════════════════╗
            ║   HireLens Cloud — Resume Ranker       ║
            ║   http://localhost:8080                ║
            ║   Upload JD + PDFs → ranked results    ║
            ╚══════════════════════════════════════════╝
            """);
    }
}
