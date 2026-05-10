package com.xcelerate

import com.sun.jna.Library
import java.lang.annotation.Native

/**
 * JNA 브릿지 — C++ 라이브러리 함수를 직접 매핑
 */
internal interface ExcelLib : Library {

    fun generateExcel(outputPath: String, rowCount: Int): Int

    companion object {
        val INSTANCE: ExcelLib by lazy {
            Native.load("xcelerate", ExcelLib::class.java)
        }
    }
}
