package com.hirelens.model;

import java.util.List;

public class AnalysisResult {
    public String fileName;
    public String candidateName;
    public double score; // 0-100
    public int matchedKeywords;
    public List<String> matchedSkills;
    public List<String> missingSkills;
    public List<String> allSkillsFound;
    public String snippet; // first 400 chars of resume
    public String verdict; // Strong Fit / Moderate / Weak
    public double tfidfScore;
    public double skillScore;
    public int wordCount;

    public AnalysisResult() {}
}
