# Xcelerate

> C++과 JNA 기반의 고성능 Excel / CSV 스트리밍 생성 라이브러리

Java/Spring 환경에서 수백만 건의 데이터를 메모리 부담 없이 빠르게 Excel·CSV 파일로 생성할 수 있습니다.

---

## 특징

- **고성능** — C++ 네이티브 코어가 DEFLATE 압축을 직접 수행, 수백만 행을 빠르게 처리
- **O(1) 메모리** — 전체 데이터를 메모리에 올리지 않고 청크 단위로 스트리밍
- **JVM 친화적** — JNA 브릿지를 통해 Java/Kotlin/Spring에서 의존성 하나로 사용
- **경량** — 별도 서버 없이 라이브러리 형태로 바로 사용 가능
- **다중 시트 자동 분리** — Excel 행 수 제한(100만 행)을 초과하면 시트를 자동으로 분리

---

## 설치

> 현재 개발 중입니다. Maven Central 배포 예정입니다.

```groovy
// build.gradle
repositories {
    mavenLocal()
}

dependencies {
    implementation 'com.xcelerate:xcelerate:0.0.1'
}
```

---

## 사용법

### 기본 — Excel 생성

```java
List<String> headers = List.of("ID", "이름", "금액");

try (ExcelWriter writer = ExcelWriter.headers(headers)
                                     .type(FileType.EXCEL)
                                     .to(outputStream)) {

    writer.append(List.of(
        List.of("1", "홍길동", "10000"),
        List.of("2", "김철수", "20000")
    ));
}
```

### 기본 — CSV 생성

```java
try (ExcelWriter writer = ExcelWriter.headers(headers)
                                     .type(FileType.CSV)
                                     .to(outputStream)) {
    writer.append(rows);
}
```

### 대용량 스트리밍 (Spring Boot + DB)

`append()`는 여러 번 호출할 수 있습니다. DB에서 배치 단위로 읽어 즉시 전달하면 메모리를 O(1)로 유지할 수 있습니다.

```java
// 1) ExcelWriter 세션 오픈 — C++ 네이티브 세션이 생성되고 OutputStream 과 연결됨
try (ExcelWriter writer = ExcelWriter.headers(headers)
                                     .type(FileType.EXCEL)
                                     .to(response.getOutputStream())) {

    // 2) DB에서 N건씩 읽어 append() — C++이 받는 즉시 압축하여 OutputStream 에 write
    repository.streamInBatches(10_000, batch -> {
        try {
            writer.append(toRows(batch));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });

} // 3) close() 시 ZIP central directory 등 xlsx 마무리 구조 기록
```

### Spring MVC 다운로드 엔드포인트

```java
@GetMapping("/download/excel")
public void downloadExcel(HttpServletResponse response) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=\"report.xlsx\"");
    exportService.exportToExcel(response.getOutputStream());
}

@GetMapping("/download/csv")
public void downloadCsv(HttpServletResponse response) throws IOException {
    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"report.csv\"");
    exportService.exportToCsv(response.getOutputStream());
}
```

---

## API

### `ExcelWriter.Builder`

| 메서드 | 설명 |
|--------|------|
| `ExcelWriter.headers(List<String>)` | 헤더 컬럼 지정, 빌더 반환 |
| `.type(FileType)` | `FileType.EXCEL` 또는 `FileType.CSV` |
| `.to(OutputStream)` | 세션 오픈, `ExcelWriter` 반환 |

### `ExcelWriter`

| 메서드 | 설명 |
|--------|------|
| `append(List<List<String>>)` | 행 데이터 추가 (여러 번 호출 가능) |
| `close()` | 세션 종료 및 파일 마무리 (try-with-resources 권장) |

---

## 프로젝트 구조

```
xcelerate/
 ├── core/           # C++ 네이티브 코어 (zlib DEFLATE 스트리밍)
 └── java-bridge/    # JNA 브릿지 및 Java API
```

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| 네이티브 코어 | C++17, zlib, CMake |
| JVM 브릿지 | Java 17, JNA 5.14 |
| 빌드 | Gradle, CMake |

---

## 라이선스

MIT License © 2026 Xcelerate
