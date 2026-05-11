#include "excel_generator.h"

extern "C" {

int generateExcel(const char* output_path, int row_count) {
    if (output_path == nullptr || row_count <= 0) {
        return 1;
    }

    // TODO: OpenXLSX 연동 후 실제 Excel 생성 구현
    return 0;
}

}
