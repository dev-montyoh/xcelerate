#pragma once

#ifdef _WIN32
#define XCELERATE_API __declspec(dllexport)
#else
#define XCELERATE_API __attribute__((visibility("default")))
#endif

extern "C" {
    /**
     * Excel 파일 생성
     * @param output_path 생성할 파일 경로
     * @param row_count   생성할 행 수
     * @return 0: 성공, 1: 실패
     */
    XCELERATE_API int generateExcel(const char* output_path, int row_count);
}
