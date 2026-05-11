package com.xcelerate;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * JNA 브릿지 — C++ 라이브러리 함수를 직접 매핑
 */
interface ExcelLib extends Library {

    ExcelLib INSTANCE = Native.load("xcelerate", ExcelLib.class);

    int generateExcel(String outputPath, int rowCount);
}
