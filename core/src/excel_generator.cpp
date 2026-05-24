#include "excel_generator.h"
#include <zlib.h>
#include <cstring>
#include <cstdint>
#include <string>
#include <vector>
#include <stdexcept>

// ============================================================
// Streaming ZIP writer
// ============================================================
//
// ZIP 스펙: Local file header + compressed data + [data descriptor] 를
// 순서대로 쓰고, 마지막에 Central Directory를 붙인다.
// 시트 XML처럼 크기를 미리 알 수 없는 항목은 General Purpose Bit 3
// (data descriptor 사용)을 세팅한 뒤 CRC/크기를 0으로 쓰고,
// 압축 완료 후 Data Descriptor로 실제 값을 추가한다.
// Central Directory에는 항상 실제 값이 기록되므로 모든 ZIP 리더가 정상 인식한다.

static void write_le16(uint8_t* buf, uint16_t v) {
    buf[0] = v & 0xFF;
    buf[1] = (v >> 8) & 0xFF;
}
static void write_le32(uint8_t* buf, uint32_t v) {
    buf[0] =  v        & 0xFF;
    buf[1] = (v >>  8) & 0xFF;
    buf[2] = (v >> 16) & 0xFF;
    buf[3] = (v >> 24) & 0xFF;
}

class ZipStreamWriter {
public:
    ZipStreamWriter(xcelerate_write_fn fn, void* ctx)
        : write_fn_(fn), ctx_(ctx), total_out_(0) {}

    // 소형 정적 파일을 비압축(store)으로 추가
    void addFile(const std::string& name, const std::string& content) {
        uint32_t crc = (uint32_t)crc32(0,
            reinterpret_cast<const Bytef*>(content.data()),
            (uInt)content.size());

        EntryInfo info;
        info.name        = name;
        info.offset      = total_out_;
        info.crc32       = crc;
        info.comp_size   = (uint32_t)content.size();
        info.uncomp_size = (uint32_t)content.size();
        info.method      = 0;

        writeLocalHeader(name, crc,
            (uint32_t)content.size(),
            (uint32_t)content.size(),
            0, false);
        emit(content.data(), content.size());
        entries_.push_back(info);
    }

    // 스트리밍 항목 시작 (DEFLATE, data descriptor 사용)
    void beginEntry(const std::string& name) {
        cur_         = EntryInfo{};
        cur_.name    = name;
        cur_.offset  = total_out_;
        cur_.method  = 8;

        writeLocalHeader(name, 0, 0, 0, 8, true);

        memset(&zstrm_, 0, sizeof(zstrm_));
        // -15: raw deflate (ZIP은 wrapperless deflate 사용)
        deflateInit2(&zstrm_, 1, Z_DEFLATED, -15, 8, Z_DEFAULT_STRATEGY);
        cur_crc_    = crc32(0, nullptr, 0);
        cur_comp_   = 0;
        cur_uncomp_ = 0;
    }

    // 스트리밍 항목에 데이터 추가 → 즉시 압축 후 콜백
    void appendData(const void* data, size_t len) {
        if (len == 0) return;
        cur_crc_    = crc32(cur_crc_, reinterpret_cast<const Bytef*>(data), (uInt)len);
        cur_uncomp_ += (uint32_t)len;

        zstrm_.next_in  = reinterpret_cast<Bytef*>(const_cast<void*>(data));
        zstrm_.avail_in = (uInt)len;

        while (zstrm_.avail_in > 0) {
            uint8_t zbuf[65536];
            zstrm_.next_out  = zbuf;
            zstrm_.avail_out = sizeof(zbuf);
            deflate(&zstrm_, Z_NO_FLUSH);
            size_t produced = sizeof(zbuf) - zstrm_.avail_out;
            if (produced > 0) {
                emit(zbuf, produced);
                cur_comp_ += (uint32_t)produced;
            }
        }
    }

    // 스트리밍 항목 종료 → deflate flush + Data Descriptor
    void endEntry() {
        int ret = Z_OK;
        while (ret != Z_STREAM_END) {
            uint8_t zbuf[65536];
            zstrm_.next_out  = zbuf;
            zstrm_.avail_out = sizeof(zbuf);
            zstrm_.next_in   = nullptr;
            zstrm_.avail_in  = 0;
            ret = deflate(&zstrm_, Z_FINISH);
            size_t produced = sizeof(zbuf) - zstrm_.avail_out;
            if (produced > 0) {
                emit(zbuf, produced);
                cur_comp_ += (uint32_t)produced;
            }
        }
        deflateEnd(&zstrm_);

        // Data Descriptor (sig + crc + comp + uncomp)
        uint8_t dd[16];
        write_le32(dd +  0, 0x08074b50u);
        write_le32(dd +  4, cur_crc_);
        write_le32(dd +  8, cur_comp_);
        write_le32(dd + 12, cur_uncomp_);
        emit(dd, 16);

        cur_.crc32       = cur_crc_;
        cur_.comp_size   = cur_comp_;
        cur_.uncomp_size = cur_uncomp_;
        entries_.push_back(cur_);
    }

    // Central Directory + End of Central Directory 기록
    void finalize() {
        uint32_t cd_offset = total_out_;
        uint32_t cd_size   = 0;

        for (auto& e : entries_) {
            uint16_t name_len = (uint16_t)e.name.size();
            uint8_t cd[46];
            write_le32(cd +  0, 0x02014b50u);
            write_le16(cd +  4, 20);       // version made by
            write_le16(cd +  6, 20);       // version needed
            write_le16(cd +  8, 0);        // flags (실제 크기는 CD에 있으므로 불필요)
            write_le16(cd + 10, e.method);
            write_le16(cd + 12, 0);        // mod time
            write_le16(cd + 14, 0);        // mod date
            write_le32(cd + 16, e.crc32);
            write_le32(cd + 20, e.comp_size);
            write_le32(cd + 24, e.uncomp_size);
            write_le16(cd + 28, name_len);
            write_le16(cd + 30, 0);        // extra field len
            write_le16(cd + 32, 0);        // comment len
            write_le16(cd + 34, 0);        // disk number start
            write_le16(cd + 36, 0);        // internal attr
            write_le32(cd + 38, 0);        // external attr
            write_le32(cd + 42, e.offset);
            emit(cd, 46);
            emit(e.name.data(), name_len);
            cd_size += 46 + name_len;
        }

        uint16_t num_entries = (uint16_t)entries_.size();
        uint8_t eocd[22];
        write_le32(eocd +  0, 0x06054b50u);
        write_le16(eocd +  4, 0);
        write_le16(eocd +  6, 0);
        write_le16(eocd +  8, num_entries);
        write_le16(eocd + 10, num_entries);
        write_le32(eocd + 12, cd_size);
        write_le32(eocd + 16, cd_offset);
        write_le16(eocd + 20, 0);
        emit(eocd, 22);
    }

private:
    struct EntryInfo {
        std::string name;
        uint32_t    offset{}, crc32{}, comp_size{}, uncomp_size{};
        uint16_t    method{};
    };

    xcelerate_write_fn write_fn_;
    void*              ctx_;
    uint32_t           total_out_;
    std::vector<EntryInfo> entries_;

    EntryInfo cur_;
    z_stream  zstrm_{};
    uint32_t  cur_crc_{}, cur_comp_{}, cur_uncomp_{};

    void emit(const void* data, size_t len) {
        write_fn_(reinterpret_cast<const char*>(data), (int)len, ctx_);
        total_out_ += (uint32_t)len;
    }

    void writeLocalHeader(const std::string& name,
                          uint32_t crc, uint32_t comp, uint32_t uncomp,
                          uint16_t method, bool use_data_descriptor) {
        uint16_t name_len = (uint16_t)name.size();
        uint8_t lh[30];
        write_le32(lh +  0, 0x04034b50u);
        write_le16(lh +  4, 20);
        write_le16(lh +  6, use_data_descriptor ? 0x08 : 0x00);
        write_le16(lh +  8, method);
        write_le16(lh + 10, 0);
        write_le16(lh + 12, 0);
        write_le32(lh + 14, use_data_descriptor ? 0 : crc);
        write_le32(lh + 18, use_data_descriptor ? 0 : comp);
        write_le32(lh + 22, use_data_descriptor ? 0 : uncomp);
        write_le16(lh + 26, name_len);
        write_le16(lh + 28, 0);
        emit(lh, 30);
        emit(name.data(), name_len);
    }
};

// ============================================================
// XML utilities
// ============================================================

static std::string xmlEscape(const char* s) {
    std::string out;
    out.reserve(64);
    for (const char* p = s; *p; ++p) {
        switch (*p) {
            case '&':  out += "&amp;";  break;
            case '<':  out += "&lt;";   break;
            case '>':  out += "&gt;";   break;
            case '"':  out += "&quot;"; break;
            case '\'': out += "&apos;"; break;
            default:   out += *p;       break;
        }
    }
    return out;
}

static std::string colLetter(int col) {
    std::string result;
    col++;
    while (col > 0) {
        col--;
        result = static_cast<char>('A' + col % 26) + result;
        col /= 26;
    }
    return result;
}

static std::string cellRef(int row_1based, int col_0based) {
    return colLetter(col_0based) + std::to_string(row_1based);
}

// ============================================================
// xlsx 정적 파일 생성
// ============================================================

static std::string makeContentTypes(int sheet_count) {
    std::string s =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n"
        "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n"
        "  <Default Extension=\"xml\"  ContentType=\"application/xml\"/>\n"
        "  <Override PartName=\"/xl/workbook.xml\""
        " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n"
        "  <Override PartName=\"/xl/styles.xml\""
        " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>\n";
    for (int i = 1; i <= sheet_count; i++) {
        s += "  <Override PartName=\"/xl/worksheets/sheet" + std::to_string(i) + ".xml\""
             " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n";
    }
    s += "</Types>";
    return s;
}

static std::string makeRels() {
    return
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
        "  <Relationship Id=\"rId1\""
        " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\""
        " Target=\"xl/workbook.xml\"/>\n"
        "</Relationships>";
}

static std::string makeWorkbook(int sheet_count) {
    std::string s =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"\n"
        "          xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n"
        "  <sheets>\n";
    for (int i = 1; i <= sheet_count; i++) {
        std::string name = (sheet_count == 1) ? "Sheet1" : "Sheet" + std::to_string(i);
        s += "    <sheet name=\"" + name + "\" sheetId=\"" + std::to_string(i)
           + "\" r:id=\"rId" + std::to_string(i) + "\"/>\n";
    }
    s += "  </sheets>\n</workbook>";
    return s;
}

static std::string makeWorkbookRels(int sheet_count) {
    std::string s =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n";
    for (int i = 1; i <= sheet_count; i++) {
        s += "  <Relationship Id=\"rId" + std::to_string(i) + "\""
             " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\""
             " Target=\"worksheets/sheet" + std::to_string(i) + ".xml\"/>\n";
    }
    s += "  <Relationship Id=\"rId" + std::to_string(sheet_count + 1) + "\""
         " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\""
         " Target=\"styles.xml\"/>\n";
    s += "</Relationships>";
    return s;
}

static const char* STYLES_XML =
    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
    "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n"
    "  <fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>\n"
    "  <fills count=\"2\">\n"
    "    <fill><patternFill patternType=\"none\"/></fill>\n"
    "    <fill><patternFill patternType=\"gray125\"/></fill>\n"
    "  </fills>\n"
    "  <borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>\n"
    "  <cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>\n"
    "  <cellXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs>\n"
    "</styleSheet>";

// ============================================================
// xlsx 스트리밍 세션
// ============================================================

static const int MAX_ROWS_PER_SHEET = 1'000'000;

class XlsxSession {
public:
    XlsxSession(const char** headers, int header_count,
                xcelerate_write_fn fn, void* ctx)
        : zip_(fn, ctx), sheet_num_(1), cur_row_(2)
    {
        for (int i = 0; i < header_count; i++)
            headers_.push_back(headers[i]);
        startSheet();
    }

    int writeRows(const char** data, int rows, int cols) {
        std::string xml;
        xml.reserve(std::min(rows, 1000) * cols * 40);

        for (int r = 0; r < rows; r++) {
            // 100만 행 초과 시 새 시트
            if (cur_row_ > MAX_ROWS_PER_SHEET + 1) {
                if (!xml.empty()) { zip_.appendData(xml.data(), xml.size()); xml.clear(); }
                endSheet();
                sheet_num_++;
                startSheet();
            }

            xml += "<row r=\"";
            xml += std::to_string(cur_row_);
            xml += "\">";
            for (int c = 0; c < cols; c++) {
                xml += "<c r=\"";
                xml += cellRef(cur_row_, c);
                xml += "\" t=\"inlineStr\"><is><t>";
                xml += xmlEscape(data[r * cols + c]);
                xml += "</t></is></c>";
            }
            xml += "</row>\n";
            cur_row_++;

            // 매 1000행마다 flush → 콜백으로 전달
            if ((r & 0x3FF) == 0x3FF) {
                zip_.appendData(xml.data(), xml.size());
                xml.clear();
            }
        }
        if (!xml.empty()) zip_.appendData(xml.data(), xml.size());
        return 0;
    }

    int close() {
        endSheet();
        int sc = sheet_num_;
        zip_.addFile("[Content_Types].xml",      makeContentTypes(sc));
        zip_.addFile("_rels/.rels",              makeRels());
        zip_.addFile("xl/workbook.xml",          makeWorkbook(sc));
        zip_.addFile("xl/_rels/workbook.xml.rels", makeWorkbookRels(sc));
        zip_.addFile("xl/styles.xml",            STYLES_XML);
        zip_.finalize();
        return 0;
    }

private:
    ZipStreamWriter          zip_;
    std::vector<std::string> headers_;
    int                      sheet_num_;
    int                      cur_row_;

    void startSheet() {
        std::string name = "xl/worksheets/sheet" + std::to_string(sheet_num_) + ".xml";
        zip_.beginEntry(name);

        std::string hdr =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            "<sheetData>\n";

        // 헤더 행
        hdr += "<row r=\"1\">";
        for (int c = 0; c < (int)headers_.size(); c++) {
            hdr += "<c r=\"" + cellRef(1, c) + "\" t=\"inlineStr\"><is><t>"
                +  xmlEscape(headers_[c].c_str()) + "</t></is></c>";
        }
        hdr += "</row>\n";

        zip_.appendData(hdr.data(), hdr.size());
        cur_row_ = 2;
    }

    void endSheet() {
        const char* footer = "</sheetData></worksheet>";
        zip_.appendData(footer, strlen(footer));
        zip_.endEntry();
    }
};

// ============================================================
// CSV 스트리밍 세션
// ============================================================

class CsvSession {
public:
    CsvSession(const char** headers, int header_count,
               xcelerate_write_fn fn, void* ctx)
        : write_fn_(fn), ctx_(ctx)
    {
        std::string line;
        line.reserve(header_count * 20);
        for (int i = 0; i < header_count; i++) {
            if (i) line += ',';
            line += escapeCsv(headers[i]);
        }
        line += '\n';
        emit(line);
    }

    int writeRows(const char** data, int rows, int cols) {
        std::string buf;
        buf.reserve(std::min(rows, 5000) * cols * 20);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (c) buf += ',';
                buf += escapeCsv(data[r * cols + c]);
            }
            buf += '\n';
            if ((r & 0xFFF) == 0xFFF) { emit(buf); buf.clear(); }
        }
        if (!buf.empty()) emit(buf);
        return 0;
    }

    int close() { return 0; }

private:
    xcelerate_write_fn write_fn_;
    void*              ctx_;

    static std::string escapeCsv(const char* val) {
        std::string s = "\"";
        for (const char* p = val; *p; ++p) {
            if (*p == '"') s += "\"\"";
            else           s += *p;
        }
        s += '"';
        return s;
    }

    void emit(const std::string& s) {
        write_fn_(s.data(), (int)s.size(), ctx_);
    }
};

// ============================================================
// C API
// ============================================================

struct Session {
    enum Type { EXCEL, CSV } type;
    union {
        XlsxSession* xlsx;
        CsvSession*  csv;
    };
};

extern "C" {

void* xcelerateOpen(
    const char**       headers,
    int                header_count,
    XcelerateFileType  file_type,
    xcelerate_write_fn write_fn,
    void*              write_ctx
) {
    if (!headers || header_count <= 0 || !write_fn) return nullptr;
    try {
        auto* s = new Session();
        if (file_type == XCELERATE_EXCEL) {
            s->type = Session::EXCEL;
            s->xlsx = new XlsxSession(headers, header_count, write_fn, write_ctx);
        } else {
            s->type = Session::CSV;
            s->csv  = new CsvSession(headers, header_count, write_fn, write_ctx);
        }
        return s;
    } catch (...) { return nullptr; }
}

int xcelerateWrite(void* session, const char** data, int row_count, int col_count) {
    if (!session || !data || row_count <= 0 || col_count <= 0) return 1;
    auto* s = static_cast<Session*>(session);
    try {
        return (s->type == Session::EXCEL)
            ? s->xlsx->writeRows(data, row_count, col_count)
            : s->csv ->writeRows(data, row_count, col_count);
    } catch (...) { return 1; }
}

int xcelerateClose(void* session) {
    if (!session) return 1;
    auto* s = static_cast<Session*>(session);
    int ret = 0;
    try {
        if (s->type == Session::EXCEL) { ret = s->xlsx->close(); delete s->xlsx; }
        else                           { ret = s->csv ->close(); delete s->csv;  }
    } catch (...) { ret = 1; }
    delete s;
    return ret;
}

} // extern "C"
