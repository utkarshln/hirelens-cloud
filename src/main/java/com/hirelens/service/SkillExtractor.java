package com.hirelens.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Curated skill taxonomy + regex extraction.
 * Covers: Languages, Frontend, Backend, Cloud, Data, DevOps, Soft slices.
 */
@Component
public class SkillExtractor {

    // 80+ canonical skills mapped to aliases
    public static final Map<String, List<String>> SKILL_ALIASES = new LinkedHashMap<>();
    static {
        add("Java", "java", "jdk", "jvm");
        add("Python", "python", "py");
        add("JavaScript", "javascript", "js", "es6");
        add("TypeScript", "typescript", "ts");
        add("React", "react", "react.js", "reactjs", "next.js", "nextjs");
        add("Angular", "angular");
        add("Vue", "vue", "vue.js");
        add("Node.js", "node", "node.js", "nodejs", "express");
        add("Spring Boot", "spring boot", "spring-boot", "spring");
        add("Spring", "spring", "spring mvc");
        add("Django", "django");
        add("Flask", "flask");
        add("SQL", "sql", "mysql", "postgresql", "postgres", "oracle sql");
        add("MongoDB", "mongodb", "mongo");
        add("Redis", "redis");
        add("PostgreSQL", "postgresql", "postgres");
        add("AWS", "aws", "amazon web services", "ec2", "s3", "lambda");
        add("Docker", "docker", "containerization");
        add("Kubernetes", "kubernetes", "k8s", "eks");
        add("CI/CD", "ci/cd", "cicd", "jenkins", "github actions", "gitlab ci");
        add("Git", "git", "github", "gitlab", "bitbucket");
        add("REST", "rest", "rest api", "restful");
        add("GraphQL", "graphql");
        add("Microservices", "microservices", "microservice");
        add("Kafka", "kafka", "event streaming");
        add("RabbitMQ", "rabbitmq");
        add("Machine Learning", "machine learning", "ml", "deep learning", "neural network");
        add("TensorFlow", "tensorflow", "tf");
        add("PyTorch", "pytorch");
        add("Pandas", "pandas");
        add("NumPy", "numpy");
        add("Tableau", "tableau");
        add("Power BI", "power bi", "powerbi");
        add("Agile", "agile", "scrum", "kanban");
        add("TDD", "tdd", "test driven");
        add("HTML", "html", "html5");
        add("CSS", "css", "css3", "tailwind", "bootstrap");
        add("Figma", "figma");
        add("JIRA", "jira");
        add("Linux", "linux", "unix");
        add("Azure", "azure");
        add("GCP", "gcp", "google cloud");
        add("Terraform", "terraform", "iac");
        add("Ansible", "ansible");
        add("Spark", "spark", "pyspark");
        add("Hadoop", "hadoop");
        add("Airflow", "airflow");
        add("ETL", "etl");
        add("Data Analysis", "data analysis", "data analytics");
        add("NLP", "nlp", "natural language processing");
        add("Computer Vision", "computer vision", "opencv");
        add("LLM", "llm", "large language model", "genai", "generative ai", "rag", "langchain");
        add("Prompt Engineering", "prompt engineering");
        add("Selenium", "selenium");
        add("Cypress", "cypress");
        add("Communication", "communication");
        add("Leadership", "leadership", "team lead");
    }

    private static void add(String canonical, String... aliases) {
        SKILL_ALIASES.put(canonical, Arrays.asList(aliases));
    }

    public Set<String> extract(String text) {
        String low = text.toLowerCase();
        Set<String> found = new LinkedHashSet<>();
        for (var e : SKILL_ALIASES.entrySet()) {
            for (String alias : e.getValue()) {
                Pattern p = Pattern.compile("\\b" + Pattern.quote(alias) + "\\b");
                if (p.matcher(low).find()) { found.add(e.getKey()); break; }
            }
        }
        return found;
    }

    public Set<String> extractRequiredSkills(String jdText) {
        Set<String> all = extract(jdText);
        // boost: if JD explicitly lists "Requirements:" section, we take all; else top 15 by frequency heuristic
        return all;
    }

    public List<String> canonicalList() { return new ArrayList<>(SKILL_ALIASES.keySet()); }
}
