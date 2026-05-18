package com.xcelerate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ExcelGeneratorTest {

    private static final String OUTPUT_PATH = "test-output";

    @AfterEach
    void cleanup() {
        new File(OUTPUT_PATH + ".xlsx").delete();
        new File(OUTPUT_PATH + ".csv").delete();
        for (int i = 1; i <= 10; i++) {
            new File(OUTPUT_PATH + "_" + i + ".csv").delete();
        }
    }

    // =========================================================
    // EXCEL - file()
    // =========================================================

    @Test
    @DisplayName("[EXCEL/file/List] 단일 시트 생성")
    void testExcelFileWithList() {
        List<String> headers = List.of("이름", "나이", "이메일");
        List<List<String>> data = List.of(
            List.of("홍길동", "30", "hong@email.com"),
            List.of("김철수", "25", "kim@email.com")
        );

        assertDoesNotThrow(() ->
            ExcelGenerator.file(OUTPUT_PATH + ".xlsx", FileType.EXCEL, headers, data)
        );

        assertTrue(new File(OUTPUT_PATH + ".xlsx").exists());
    }

    @Test
    @DisplayName("[EXCEL/file/Stream] Java Stream으로 파일 생성")
    void testExcelFileWithStream() {
        List<String> headers = List.of("이름", "나이");
        Stream<List<String>> dataStream = Stream.of(
            List.of("홍길동", "30"),
            List.of("김철수", "25")
        );

        assertDoesNotThrow(() ->
            ExcelGenerator.file(OUTPUT_PATH + ".xlsx", FileType.EXCEL, headers, dataStream)
        );

        assertTrue(new File(OUTPUT_PATH + ".xlsx").exists());
    }

    @Test
    @DisplayName("[EXCEL/file] 100만 행 초과 시 자동 시트 분리")
    void testExcelAutoSheetSplit() {
        List<String> headers = List.of("번호", "값");
        List<List<String>> data = new java.util.ArrayList<>();
        for (int i = 0; i < ExcelGenerator.MAX_ROWS_PER_SHEET + 10; i++) {
            data.add(List.of(String.valueOf(i), "value" + i));
        }

        assertDoesNotThrow(() ->
            ExcelGenerator.file(OUTPUT_PATH + ".xlsx", FileType.EXCEL, headers, data)
        );

        assertTrue(new File(OUTPUT_PATH + ".xlsx").exists());
    }

    // =========================================================
    // EXCEL - stream()
    // =========================================================

    @Test
    @DisplayName("[EXCEL/stream/List] OutputStream으로 스트리밍")
    void testExcelStreamWithList() throws Exception {
        List<String> headers = List.of("이름", "나이");
        List<List<String>> data = List.of(
            List.of("홍길동", "30"),
            List.of("김철수", "25")
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertDoesNotThrow(() ->
            ExcelGenerator.stream(out, FileType.EXCEL, headers, data)
        );

        assertTrue(out.size() > 0);
    }

    @Test
    @DisplayName("[EXCEL/stream/Stream] Java Stream으로 스트리밍")
    void testExcelStreamWithJavaStream() {
        List<String> headers = List.of("이름", "나이");
        Stream<List<String>> dataStream = Stream.of(
            List.of("홍길동", "30"),
            List.of("김철수", "25")
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertDoesNotThrow(() ->
            ExcelGenerator.stream(out, FileType.EXCEL, headers, dataStream)
        );

        assertTrue(out.size() > 0);
    }

    // =========================================================
    // CSV - file()
    // =========================================================

    @Test
    @DisplayName("[CSV/file/List] 단일 파일 생성")
    void testCsvFileWithList() {
        List<String> headers = List.of("이름", "나이");
        List<List<String>> data = List.of(
            List.of("홍길동", "30"),
            List.of("김철수", "25")
        );

        assertDoesNotThrow(() ->
            ExcelGenerator.file(OUTPUT_PATH, FileType.CSV, headers, data)
        );

        assertTrue(new File(OUTPUT_PATH + ".csv").exists());
    }

    @Test
    @DisplayName("[CSV/file] 100만 행 초과 시 자동 파일 분리")
    void testCsvAutoFileSplit() {
        List<String> headers = List.of("번호", "값");
        List<List<String>> data = new java.util.ArrayList<>();
        for (int i = 0; i < ExcelGenerator.MAX_ROWS_PER_SHEET + 10; i++) {
            data.add(List.of(String.valueOf(i), "value" + i));
        }

        assertDoesNotThrow(() ->
            ExcelGenerator.file(OUTPUT_PATH, FileType.CSV, headers, data)
        );

        assertTrue(new File(OUTPUT_PATH + "_1.csv").exists());
        assertTrue(new File(OUTPUT_PATH + "_2.csv").exists());
    }

    // =========================================================
    // CSV - stream()
    // =========================================================

    @Test
    @DisplayName("[CSV/stream/List] zip으로 묶어서 OutputStream으로 스트리밍")
    void testCsvStreamWithList() throws Exception {
        List<String> headers = List.of("이름", "나이");
        List<List<String>> data = List.of(
            List.of("홍길동", "30"),
            List.of("김철수", "25")
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertDoesNotThrow(() ->
            ExcelGenerator.stream(out, FileType.CSV, headers, data)
        );

        // zip 파일 검증
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertNotNull(zip.getNextEntry());
    }
}
