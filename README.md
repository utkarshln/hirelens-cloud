# HireLens Cloud — Resume Ranker

[![Deploy on Railway](https://railway.app/button.svg)](https://railway.app/new/template?template=https://github.com/utkarshln/hirelens-cloud)

Interactive Java web app: paste JD + drop 100 PDFs → instant ranked table + skill heatmap + missing skills. Built for Railway/Vercel.

**Live locally:** http://localhost:8080 — health at `/health`

## Stack
Java 21, Spring Boot 3.3.5, PDFBox 3.0, Tika, Tailwind + vanilla JS, Dockerfile

## Features
- Drag-drop 100 PDFs (PDFBox text extraction, fallback for scanned)
- Skill taxonomy 80+ with aliases → matched / missing per candidate
- TF-IDF cosine (60%) + skill coverage (40%) → blended 0-100 score
- Heatmap demand coverage, verdict Strong/Moderate/Weak
- Export CSV, live filter, snippet copy
- Demo JD + generate 6 demo PDFs (no upload needed to try)

## Run locally
```bash
mvn package -DskipTests
java -jar target/hirelens-1.0.0.jar
# http://localhost:8080
```

## API
```
POST /api/analyze  (multipart: jd, files[])
GET  /api/skills
GET  /api/demo-jd
GET  /health
```

## Deploy to Railway
```bash
railway init
railway up
# env PORT auto-injected, Dockerfile handles healthcheck at /health
```
Or Docker: `docker build -t hirelens . && docker run -p 8080:8080 hirelens`

## Deploy to Vercel (frontend)
Frontend is static Thymeleaf; for Vercel split: keep backend on Railway, deploy frontend folder as static, set `NEXT_PUBLIC_API_URL`.

## Project
```
src/main/java/com/hirelens/
  HireLensApplication.java
  controller/ApiController.java, PageController.java
  service/RankingService.java, SkillExtractor.java, PdfExtractor.java
  model/AnalysisResult.java
src/main/resources/templates/index.html
Dockerfile, railway.json
```
