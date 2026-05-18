package com.xcelerate;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * JNA 브릿지 — C++ 라이브러리 함수를 직접 매핑
 */
interface ExcelLib extends Library {

    ExcelLib INSTANCE = Native.load("xcelerate", ExcelLib.class);

    /**
     * @param outputPath  생성할 파일 경로
     * @param sheetNames  시트 이름 배열
     * @param sheetCount  시트 수
     * @param headers     헤더 배열
     * @param headerCount 헤더 수
     * @param data        데이터 배열 (flatten)
     * @param rowCount    시트당 행 수
     * @return 0: 성공, 1: 실패
     */
    int generateExcel(
        String outputPath,
        String[] sheetNames,
        int sheetCount,
        String[] headers,
        int headerCount,
        String[] data,
        int rowCount
    );
}
