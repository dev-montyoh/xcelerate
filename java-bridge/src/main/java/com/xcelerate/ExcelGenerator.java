package com.xcelerate;

/**
 * 외부에서 사용하는 API
 * Spring 등에서 이 클래스만 사용하면 됨
 */
public class ExcelGenerator {

    /**
     * @param outputPath 생성할 Excel 파일 경로
     * @param rowCount   생성할 행 수
     */
    public static void generate(String outputPath, int rowCount) {
        int result = ExcelLib.INSTANCE.generateExcel(outputPath, rowCount);
        if (result != 0) {
            throw new RuntimeException("Excel 생성 실패 (error code: " + result + ")");
        }
    }
}
