#include "excel_generator.h"
#include <OpenXLSX.hpp>
#include <string>
#include <vector>

using namespace OpenXLSX;

extern "C" {

int generateExcel(
    const char* output_path,
    const char** sheet_names,
    int sheet_count,
    const char** headers,
    int header_count,
    const char** data,
    int row_count
) {
    if (output_path == nullptr || sheet_names == nullptr ||
        headers == nullptr || data == nullptr ||
        sheet_count <= 0 || header_count <= 0 || row_count < 0) {
        return 1;
    }

    try {
        XLDocument doc;
        doc.create(output_path, true);

        for (int s = 0; s < sheet_count; s++) {
            std::string sheet_name = sheet_names[s];

            // 첫 번째 시트는 기본 생성된 Sheet1을 rename, 나머지는 추가
            if (s == 0) {
                doc.workbook().worksheet("Sheet1").setName(sheet_name);
            } else {
                doc.workbook().addWorksheet(sheet_name);
            }

            auto sheet = doc.workbook().worksheet(sheet_name);

            // 헤더 작성
            for (int col = 0; col < header_count; col++) {
                sheet.cell(1, col + 1).value() = headers[col];
            }

            // 데이터 작성
            // data는 [시트][행][열] 순서로 flatten된 1차원 배열
            int sheet_offset = s * row_count * header_count;
            for (int row = 0; row < row_count; row++) {
                for (int col = 0; col < header_count; col++) {
                    int idx = sheet_offset + row * header_count + col;
                    sheet.cell(row + 2, col + 1).value() = data[idx];
                }
            }
        }

        doc.save();
        doc.close();
        return 0;

    } catch (...) {
        return 1;
    }
}

}
