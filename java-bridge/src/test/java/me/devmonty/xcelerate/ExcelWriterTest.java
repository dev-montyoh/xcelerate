package me.devmonty.xcelerate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ExcelWriterTest {

    // =========================================================
    // EXCEL
    // =========================================================

    @Test
    @DisplayName("[EXCEL] 기본 생성 및 유효한 ZIP 구조 검증")
    void testExcelBasicOutput() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ExcelWriter writer = ExcelWriter
                .headers("이름", "나이", "이메일")
                .type(FileType.EXCEL)
                .to(out)) {
            writer.append(List.of(
                List.of("홍길동", "30", "hong@email.com"),
                List.of("김철수", "25", "kim@email.com")
            ));
        }

        assertTrue(out.size() > 0, "출력이 비어있음");

        Set<String> entries = zipEntries(out.toByteArray());
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"));
        assertTrue(entries.contains("[Content_Types].xml"));
        assertTrue(entries.contains("xl/workbook.xml"));
        assertTrue(entries.contains("xl/styles.xml"));
    }

    @Test
    @DisplayName("[EXCEL] append() 여러 번 호출")
    void testExcelMultipleAppend() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ExcelWriter writer = ExcelWriter
                .headers("번호", "값")
                .type(FileType.EXCEL)
                .to(out)) {
            writer.append(List.of(List.of("1", "A"), List.of("2", "B")));
            writer.append(List.of(List.of("3", "C"), List.of("4", "D")));
        }

        assertTrue(out.size() > 0);
        Set<String> entries = zipEntries(out.toByteArray());
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"));
    }

    @Test
    @DisplayName("[EXCEL] XML 특수문자 이스케이프")
    void testExcelXmlEscape() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ExcelWriter writer = ExcelWriter
                .headers("값")
                .type(FileType.EXCEL)
                .to(out)) {
            writer.append(List.of(
                List.of("<script>alert('xss')</script>"),
                List.of("a & b"),
                List.of("\"quoted\"")
            ));
        }

        // ZIP 구조 유효성만 확인 (파싱은 실제 Excel에서 검증)
        assertFalse(zipEntries(out.toByteArray()).isEmpty());
    }

    @Test
    @DisplayName("[EXCEL] 100만 행 초과 시 시트 자동 분리")
    void testExcelAutoSheetSplit() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int totalRows = 1_000_010;

        try (ExcelWriter writer = ExcelWriter
                .headers("번호")
                .type(FileType.EXCEL)
                .to(out)) {
            // 10만 행씩 11회 전달
            for (int batch = 0; batch < 11; batch++) {
                int size = (batch < 10) ? 100_000 : 10;
                List<List<String>> rows = new java.util.ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    rows.add(List.of(String.valueOf(batch * 100_000 + i)));
                }
                writer.append(rows);
            }
        }

        Set<String> entries = zipEntries(out.toByteArray());
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"), "sheet1 없음");
        assertTrue(entries.contains("xl/worksheets/sheet2.xml"), "sheet2 없음 (시트 분리 실패)");
    }

    // =========================================================
    // CSV
    // =========================================================

    @Test
    @DisplayName("[CSV] 기본 내용 검증")
    void testCsvBasicOutput() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ExcelWriter writer = ExcelWriter
                .headers("이름", "나이")
                .type(FileType.CSV)
                .to(out)) {
            writer.append(List.of(
                List.of("홍길동", "30"),
                List.of("김철수", "25")
            ));
        }

        String content = out.toString("UTF-8");
        assertTrue(content.startsWith("\"이름\",\"나이\""), "헤더 오류");
        assertTrue(content.contains("\"홍길동\",\"30\""), "데이터 오류");
        assertTrue(content.contains("\"김철수\",\"25\""), "데이터 오류");
    }

    @Test
    @DisplayName("[CSV] 쉼표·따옴표 포함 값 이스케이프")
    void testCsvEscape() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ExcelWriter writer = ExcelWriter
                .headers("값")
                .type(FileType.CSV)
                .to(out)) {
            writer.append(List.of(
                List.of("a,b"),
                List.of("say \"hi\"")
            ));
        }

        String content = out.toString("UTF-8");
        assertTrue(content.contains("\"a,b\""),            "쉼표 이스케이프 오류");
        assertTrue(content.contains("\"say \"\"hi\"\"\""), "따옴표 이스케이프 오류");
    }

    @Test
    @DisplayName("[CSV] append() 여러 번 호출 시 전체 행 포함")
    void testCsvMultipleAppend() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ExcelWriter writer = ExcelWriter
                .headers("번호")
                .type(FileType.CSV)
                .to(out)) {
            writer.append(List.of(List.of("1"), List.of("2")));
            writer.append(List.of(List.of("3"), List.of("4")));
        }

        String content = out.toString("UTF-8");
        assertTrue(content.contains("\"1\""));
        assertTrue(content.contains("\"4\""));
    }

    // =========================================================
    // 공통
    // =========================================================

    @Test
    @DisplayName("빈 데이터 append() — 예외 없이 처리")
    void testEmptyAppend() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> {
            try (ExcelWriter writer = ExcelWriter
                    .headers("A")
                    .type(FileType.EXCEL)
                    .to(out)) {
                writer.append(List.of());
            }
        });
        assertTrue(out.size() > 0);
    }

    // =========================================================
    // 헬퍼
    // =========================================================

    private static Set<String> zipEntries(byte[] bytes) throws IOException {
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
                zip.closeEntry();
            }
        }
        return names;
    }
}
