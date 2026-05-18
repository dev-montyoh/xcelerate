#pragma once

#ifdef _WIN32
#define XCELERATE_API __declspec(dllexport)
#else
#define XCELERATE_API __attribute__((visibility("default")))
#endif

extern "C" {

    /**
     * Excel 파일 생성
     *
     * @param output_path  생성할 파일 경로
     * @param sheet_names  시트 이름 배열
     * @param sheet_count  시트 수
     * @param headers      헤더 배열 (시트별 헤더, 1차원으로 flatten)
     * @param header_count 헤더 수 (시트당 동일)
     * @param data         데이터 배열 (시트별 데이터, 1차원으로 flatten)
     * @param row_count    시트당 데이터 행 수
     * @return 0: 성공, 1: 실패
     */
    XCELERATE_API int generateExcel(
        const char* output_path,
        const char** sheet_names,
        int sheet_count,
        const char** headers,
        int header_count,
        const char** data,
        int row_count
    );

}
