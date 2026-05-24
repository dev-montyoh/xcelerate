#pragma once

#ifdef _WIN32
  #define XCELERATE_API __declspec(dllexport)
#else
  #define XCELERATE_API __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*xcelerate_write_fn)(const char* data, int length, void* ctx);

typedef enum {
    XCELERATE_EXCEL = 0,
    XCELERATE_CSV   = 1
} XcelerateFileType;

/**
 * 스트리밍 세션 열기.
 * write_fn 콜백으로 생성된 바이트를 즉시 전달한다.
 * @return 세션 핸들 (NULL이면 실패)
 */
XCELERATE_API void* xcelerateOpen(
    const char**       headers,
    int                header_count,
    XcelerateFileType  file_type,
    xcelerate_write_fn write_fn,
    void*              write_ctx
);

/**
 * 행 배치 추가. data는 row-major flat 배열: data[row * col_count + col]
 * @return 0 성공, 비零 실패
 */
XCELERATE_API int xcelerateWrite(
    void*        session,
    const char** data,
    int          row_count,
    int          col_count
);

/**
 * 세션 종료 및 자원 해제. ZIP Central Directory 포함 모든 후처리 바이트를 콜백으로 전달한다.
 * @return 0 성공, 비零 실패
 */
XCELERATE_API int xcelerateClose(void* session);

#ifdef __cplusplus
}
#endif
