package com.hirelens.controller;

import com.hirelens.model.AnalysisResult;
import com.hirelens.service.RankingService;
import com.hirelens.service.SkillExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
public class ApiController {

    @Autowired private RankingService rankingService;
    @Autowired private SkillExtractor skillExtractor;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "HireLens", "version", "1.0.0");
    }

    @GetMapping("/api/skills")
    public List<String> skills() { return skillExtractor.canonicalList(); }

    @PostMapping(value = "/api/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyze(
            @RequestParam("jd") String jd,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "jdFile", required = false) MultipartFile jdFile
    ) {
        try {
            String jdText = jd;
            if (jdFile != null && !jdFile.isEmpty()) {
                String fromFile = new String(jdFile.getBytes());
                if (!fromFile.isBlank()) jdText = fromFile;
            }
            if (jdText == null || jdText.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Job description is required (paste or upload)"));
            if (files == null || files.stream().allMatch(MultipartFile::isEmpty)) return ResponseEntity.badRequest().body(Map.of("error", "Upload at least 1 resume PDF"));

            List<MultipartFile> valid = files.stream().filter(f -> !f.isEmpty()).toList();
            if (valid.size() > 100) return ResponseEntity.badRequest().body(Map.of("error", "Max 100 resumes per batch"));

            long start = System.currentTimeMillis();
            List<AnalysisResult> ranked = rankingService.rank(jdText, valid);
            long took = System.currentTimeMillis() - start;

            Set<String> requiredSkills = skillExtractor.extractRequiredSkills(jdText);
            Map<String, Long> skillDemand = new LinkedHashMap<>();
            for (String s : requiredSkills) skillDemand.put(s, ranked.stream().filter(r -> r.matchedSkills.contains(s)).count());

            Map<String,Object> resp = new LinkedHashMap<>();
            resp.put("count", ranked.size());
            resp.put("tookMs", took);
            resp.put("requiredSkills", requiredSkills);
            resp.put("skillCoverage", skillDemand);
            resp.put("results", ranked);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/api/demo-jd")
    public Map<String,String> demoJd() {
        String jd = """
                Senior Full Stack Engineer — HireLens (Remote / Bangalore)
                
                We are building HireLens, a modern hiring platform. Looking for a Senior Full Stack Engineer who can own features end-to-end.
                
                Requirements:
                - 3+ years with Java, Spring Boot, REST APIs, Microservices
                - Strong React, TypeScript, JavaScript, HTML/CSS
                - SQL, PostgreSQL, MongoDB, Redis
                - AWS, Docker, Kubernetes, CI/CD, Git
                - Experience with Kafka or RabbitMQ is a plus
                - Familiarity with Python, Machine Learning or LLM is bonus
                - Agile, Communication, Leadership
                
                Nice to have: GraphQL, Node.js, Terraform, Spark, Figma
                
                Responsibilities: Design scalable APIs, build delightful UIs, mentor juniors, own DevOps pipeline.
                """;
        return Map.of("jd", jd);
    }
}
