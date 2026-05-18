package com.xcelerate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 외부에서 사용하는 API
 *
 * 파일 타입: FileType.EXCEL / FileType.CSV
 *
 * - EXCEL: 100만 행 초과 시 자동 시트 분리
 * - CSV:   100만 행 초과 시 자동 파일 분리 (stream 시 zip으로 묶어서 반환)
 */
public class ExcelGenerator {

    static final int MAX_ROWS_PER_SHEET = 1_000_000;

    // =========================================================
    // file() - 파일 저장
    // =========================================================

    /**
     * List 기반 파일 저장
     *
     * @param outputPath EXCEL: "output.xlsx" / CSV: "output" (확장자 없이)
     * @param fileType   FileType.EXCEL or FileType.CSV
     * @param headers    헤더 목록
     * @param data       데이터 목록
     */
    public static void file(
        String outputPath,
        FileType fileType,
        List<String> headers,
        List<List<String>> data
    ) {
        file(outputPath, fileType, headers, data.stream());
    }

    /**
     * Stream 기반 파일 저장
     */
    public static void file(
        String outputPath,
        FileType fileType,
        List<String> headers,
        Stream<List<String>> dataStream
    ) {
        List<List<String>> data = dataStream.collect(Collectors.toList());

        if (fileType == FileType.EXCEL) {
            writeExcelFile(outputPath, headers, data);
        } else {
            writeCsvFiles(outputPath, headers, data);
        }
    }

    // =========================================================
    // stream() - OutputStream으로 스트리밍
    // =========================================================

    /**
     * List 기반 스트리밍
     *
     * - EXCEL: .xlsx → OutputStream
     * - CSV:   여러 .csv → zip → OutputStream
     */
    public static void stream(
        OutputStream outputStream,
        FileType fileType,
        List<String> headers,
        List<List<String>> data
    ) throws IOException {
        stream(outputStream, fileType, headers, data.stream());
    }

    /**
     * Stream 기반 스트리밍
     */
    public static void stream(
        OutputStream outputStream,
        FileType fileType,
        List<String> headers,
        Stream<List<String>> dataStream
    ) throws IOException {
        List<List<String>> data = dataStream.collect(Collectors.toList());

        if (fileType == FileType.EXCEL) {
            streamExcel(outputStream, headers, data);
        } else {
            streamCsvAsZip(outputStream, headers, data);
        }
    }

    // =========================================================
    // EXCEL 내부 구현
    // =========================================================

    private static void writeExcelFile(String outputPath, List<String> headers, List<List<String>> data) {
        List<String> sheetNames = buildSheetNames(data.size());
        List<List<List<String>>> sheetData = splitIntoChunks(data);
        List<List<String>> sheetHeaders = sheetNames.stream().map(s -> headers).collect(Collectors.toList());
        nativeGenerate(outputPath, sheetNames, sheetHeaders, sheetData);
    }

    private static void streamExcel(OutputStream outputStream, List<String> headers, List<List<String>> data)
        throws IOException {
        Path tempFile = Files.createTempFile("xcelerate-", ".xlsx");
        try {
            writeExcelFile(tempFile.toString(), headers, data);
            Files.copy(tempFile, outputStream);
            outputStream.flush();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // =========================================================
    // CSV 내부 구현
    // =========================================================

    private static void writeCsvFiles(String basePath, List<String> headers, List<List<String>> data) {
        List<List<List<String>>> chunks = splitIntoChunks(data);
        for (int i = 0; i < chunks.size(); i++) {
            String path = chunks.size() == 1 ? basePath + ".csv" : basePath + "_" + (i + 1) + ".csv";
            writeSingleCsv(path, headers, chunks.get(i));
        }
    }

    private static void writeSingleCsv(String path, List<String> headers, List<List<String>> data) {
        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
            writer.write(toCsvRow(headers));
            for (List<String> row : data) {
                writer.write(toCsvRow(row));
            }
        } catch (IOException e) {
            throw new RuntimeException("CSV 생성 실패: " + path, e);
        }
    }

    private static void streamCsvAsZip(OutputStream outputStream, List<String> headers, List<List<String>> data)
        throws IOException {
        List<List<List<String>>> chunks = splitIntoChunks(data);
        try (ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (int i = 0; i < chunks.size(); i++) {
                String fileName = chunks.size() == 1 ? "output.csv" : "output_" + (i + 1) + ".csv";
                zip.putNextEntry(new ZipEntry(fileName));
                writeCsvToStream(zip, headers, chunks.get(i));
                zip.closeEntry();
            }
        }
    }

    private static void writeCsvToStream(OutputStream out, List<String> headers, List<List<String>> data)
        throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write(toCsvRow(headers));
        for (List<String> row : data) {
            writer.write(toCsvRow(row));
        }
        writer.flush();
    }

    private static String toCsvRow(List<String> row) {
        return row.stream()
            .map(cell -> "\"" + cell.replace("\"", "\"\"") + "\"")
            .collect(Collectors.joining(",")) + "\n";
    }

    // =========================================================
    // 공통 유틸
    // =========================================================

    private static List<List<List<String>>> splitIntoChunks(List<List<String>> data) {
        List<List<List<String>>> chunks = new ArrayList<>();
        for (int i = 0; i < data.size(); i += MAX_ROWS_PER_SHEET) {
            chunks.add(data.subList(i, Math.min(i + MAX_ROWS_PER_SHEET, data.size())));
        }
        if (chunks.isEmpty()) chunks.add(new ArrayList<>());
        return chunks;
    }

    private static List<String> buildSheetNames(int totalRows) {
        int sheetCount = Math.max(1, (int) Math.ceil((double) totalRows / MAX_ROWS_PER_SHEET));
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= sheetCount; i++) {
            names.add(sheetCount == 1 ? "Sheet1" : "Sheet" + i);
        }
        return names;
    }

    // =========================================================
    // C++ 네이티브 호출
    // =========================================================

    private static void nativeGenerate(
        String outputPath,
        List<String> sheetNames,
        List<List<String>> headers,
        List<List<List<String>>> data
    ) {
        int sheetCount = sheetNames.size();
        int headerCount = headers.get(0).size();
        int rowCount = data.get(0).size();

        String[] sheetNamesArr = sheetNames.toArray(new String[0]);
        String[] headersArr = headers.get(0).toArray(new String[0]);

        String[] dataArr = new String[sheetCount * rowCount * headerCount];
        for (int s = 0; s < sheetCount; s++) {
            List<List<String>> sheetData = data.get(s);
            for (int row = 0; row < rowCount; row++) {
                List<String> rowData = sheetData.get(row);
                for (int col = 0; col < headerCount; col++) {
                    dataArr[s * rowCount * headerCount + row * headerCount + col] = rowData.get(col);
                }
            }
        }

        int result = ExcelLib.INSTANCE.generateExcel(
            outputPath,
            sheetNamesArr,
            sheetCount,
            headersArr,
            headerCount,
            dataArr,
            rowCount
        );

        if (result != 0) {
            throw new RuntimeException("Excel 생성 실패 (error code: " + result + ")");
        }
    }
}
