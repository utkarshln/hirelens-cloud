package com.hirelens.service;

import com.hirelens.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RankingService {

    @Autowired private PdfExtractor pdfExtractor;
    @Autowired private SkillExtractor skillExtractor;

    public List<AnalysisResult> rank(String jdText, List<MultipartFile> files) throws Exception {
        if (jdText == null || jdText.isBlank()) throw new IllegalArgumentException("Job description is required");
        String jdNormalized = normalize(jdText);
        Set<String> jdTokens = tokenize(jdNormalized);
        Set<String> requiredSkills = skillExtractor.extractRequiredSkills(jdText);
        Map<String, Double> jdTf = tf(jdTokens);
        // idf over JD + all resumes (simple)
        List<String> resumeTexts = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f.isEmpty()) continue;
            String text = pdfExtractor.extractText(f);
            if (text == null) text = "";
            resumeTexts.add(text);
            fileNames.add(f.getOriginalFilename());
        }
        if (resumeTexts.isEmpty()) throw new IllegalArgumentException("No readable resumes uploaded");

        // Build IDF across corpus (JD + resumes)
        List<Set<String>> corpus = new ArrayList<>();
        corpus.add(jdTokens);
        for (String t : resumeTexts) corpus.add(tokenize(normalize(t)));
        Map<String, Double> idf = computeIdf(corpus);

        Map<String, Double> jdVector = tfidfVector(jdTf, idf);

        List<AnalysisResult> results = new ArrayList<>();
        for (int i = 0; i < resumeTexts.size(); i++) {
            String raw = resumeTexts.get(i);
            String norm = normalize(raw);
            Set<String> tokens = tokenize(norm);
            Map<String, Double> tf = tf(tokens);
            Map<String, Double> vec = tfidfVector(tf, idf);
            double cosine = cosineSimilarity(jdVector, vec);

            Set<String> resumeSkills = skillExtractor.extract(raw);
            Set<String> matched = new LinkedHashSet<>(resumeSkills);
            matched.retainAll(requiredSkills);
            Set<String> missing = new LinkedHashSet<>(requiredSkills);
            missing.removeAll(resumeSkills);

            double skillScore = requiredSkills.isEmpty() ? 0 : (double) matched.size() / requiredSkills.size();
            // Blend: 60% TF-IDF cosine + 40% skill coverage
            double blended = 0.6 * cosine + 0.4 * skillScore;
            double finalScore = Math.round(blended * 1000.0) / 10.0; // 0-100 one decimal

            AnalysisResult r = new AnalysisResult();
            r.fileName = fileNames.get(i);
            r.candidateName = guessName(raw, fileNames.get(i));
            r.score = finalScore;
            r.tfidfScore = Math.round(cosine * 1000.0)/10.0;
            r.skillScore = Math.round(skillScore * 1000.0)/10.0;
            r.matchedSkills = new ArrayList<>(matched);
            r.missingSkills = new ArrayList<>(missing);
            r.allSkillsFound = new ArrayList<>(resumeSkills);
            r.matchedKeywords = matched.size();
            r.wordCount = tokens.size();
            r.snippet = raw.length() > 420 ? raw.substring(0, 420).replaceAll("\\s+", " ") + "…" : raw.replaceAll("\\s+", " ");
            if (finalScore >= 70) r.verdict = "Strong Fit";
            else if (finalScore >= 45) r.verdict = "Moderate Fit";
            else r.verdict = "Weak Fit";
            results.add(r);
        }
        results.sort(Comparator.comparingDouble((AnalysisResult x) -> x.score).reversed());
        return results;
    }

    // ——— NLP helpers ———
    private String normalize(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private Set<String> tokenize(String normalized) {
        if (normalized.isBlank()) return Set.of();
        String[] parts = normalized.split("\\s+");
        Set<String> out = new LinkedHashSet<>();
        Set<String> stop = Set.of("the","and","or","a","an","in","on","for","with","to","of","is","are","we","you","our","as","at","by","be","will","this","that","it","from","have","has","had","was","were","been","looking","seeking","need","needs","required","requirements","responsibilities","experience","years","year","role","position","team","work","working","ability","strong","good","great","excellent");
        for (String p : parts) if (p.length() > 2 && !stop.contains(p)) out.add(stem(p));
        return out;
    }

    private String stem(String w) {
        // very light stemmer
        if (w.endsWith("ing") && w.length()>5) return w.substring(0, w.length()-3);
        if (w.endsWith("tion") && w.length()>5) return w.substring(0, w.length()-4);
        if (w.endsWith("er") && w.length()>4) return w.substring(0, w.length()-2);
        if (w.endsWith("s") && w.length()>4) return w.substring(0, w.length()-1);
        return w;
    }

    private Map<String, Double> tf(Set<String> tokens) {
        Map<String, Double> m = new HashMap<>();
        if (tokens.isEmpty()) return m;
        for (String t : tokens) m.merge(t, 1.0, Double::sum);
        double n = tokens.size();
        m.replaceAll((k,v) -> v / n);
        return m;
    }

    private Map<String, Double> computeIdf(List<Set<String>> docs) {
        Map<String, Integer> df = new HashMap<>();
        for (Set<String> d : docs) for (String t : d) df.merge(t, 1, Integer::sum);
        int N = docs.size();
        Map<String, Double> idf = new HashMap<>();
        for (var e : df.entrySet()) idf.put(e.getKey(), Math.log((double)N / e.getValue()) + 1);
        return idf;
    }

    private Map<String, Double> tfidfVector(Map<String, Double> tf, Map<String, Double> idf) {
        Map<String, Double> v = new HashMap<>();
        for (var e : tf.entrySet()) v.put(e.getKey(), e.getValue() * idf.getOrDefault(e.getKey(), 1.0));
        return v;
    }

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        double dot = 0, na = 0, nb = 0;
        for (double v : a.values()) na += v*v;
        for (double v : b.values()) nb += v*v;
        for (var e : a.entrySet()) dot += e.getValue() * b.getOrDefault(e.getKey(), 0.0);
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom == 0 ? 0 : dot / denom;
    }

    private String guessName(String text, String filename) {
        // try first line that looks like a name (2 words capitalized)
        Pattern p = Pattern.compile("^\\s*([A-Z][a-z]+\\s+[A-Z][a-z]+)", Pattern.MULTILINE);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1).trim();
        if (filename != null) {
            String base = filename.replaceAll("\\.[^.]+$", "").replaceAll("[_\\-]", " ").trim();
            if (base.length() > 2 && base.length() < 40) return base;
        }
        return "Candidate";
    }
}
