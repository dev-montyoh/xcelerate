package me.devmonty.xcelerate;

import com.sun.jna.Pointer;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 스트리밍 Excel/CSV 생성기.
 *
 * <pre>{@code
 * try (ExcelWriter writer = ExcelWriter
 *         .headers("이름", "나이", "이메일")
 *         .type(FileType.EXCEL)
 *         .to(response.getOutputStream())) {
 *
 *     writer.append(batch1);
 *     writer.append(batch2);
 * }
 * }</pre>
 *
 * append() 호출마다 C++ 코어가 즉시 XML/CSV를 생성해 OutputStream으로 전달한다.
 * close() 시 ZIP Central Directory가 기록되고 스트림이 완성된다.
 */
public class ExcelWriter implements Closeable {

    private final Pointer                 session;
    private final int                     colCount;
    private final OutputStream            out;
    private final ExcelLib.WriteCallback  callback;

    private volatile Throwable writeError = null;
    private boolean closed = false;

    private ExcelWriter(List<String> headers, FileType type, OutputStream out) throws IOException {
        this.colCount = headers.size();
        this.out      = out;

        // JNA 콜백: C++가 바이트를 생성할 때마다 호출된다
        // 레퍼런스를 필드에 유지해 GC 방지
        this.callback = (data, length, ctx) -> {
            if (writeError != null) return;
            try {
                out.write(data.getByteArray(0, length));
            } catch (Throwable t) {
                writeError = t;
            }
        };

        String[] arr = headers.toArray(new String[0]);
        this.session = ExcelLib.INSTANCE.xcelerateOpen(
            arr, arr.length,
            type == FileType.EXCEL ? 0 : 1,
            callback, null
        );

        if (this.session == null) {
            throw new IllegalStateException("xcelerate 세션 생성 실패");
        }

        // 헤더 기록 시 발생한 스트림 오류 즉시 전파
        checkWriteError();
    }

    // =========================================================
    // 빌더
    // =========================================================

    public static Builder headers(String... headers) {
        return new Builder(Arrays.asList(headers));
    }

    public static Builder headers(List<String> headers) {
        return new Builder(headers);
    }

    public static final class Builder {
        private final List<String> headers;
        private FileType type = FileType.EXCEL;

        Builder(List<String> headers) {
            this.headers = headers;
        }

        public Builder type(FileType type) {
            this.type = type;
            return this;
        }

        public ExcelWriter to(OutputStream out) throws IOException {
            return new ExcelWriter(headers, type, out);
        }
    }

    // =========================================================
    // 데이터 추가
    // =========================================================

    public ExcelWriter append(List<List<String>> rows) throws IOException {
        if (rows.isEmpty()) return this;
        checkWriteError();

        int rowCount = rows.size();
        String[] flat = new String[rowCount * colCount];
        for (int r = 0; r < rowCount; r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < colCount; c++) {
                flat[r * colCount + c] = (c < row.size()) ? row.get(c) : "";
            }
        }

        if (ExcelLib.INSTANCE.xcelerateWrite(session, flat, rowCount, colCount) != 0) {
            throw new IOException("xcelerateWrite 실패");
        }

        checkWriteError();
        return this;
    }

    // =========================================================
    // 종료
    // =========================================================

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;

        checkWriteError();

        if (ExcelLib.INSTANCE.xcelerateClose(session) != 0) {
            throw new IOException("xcelerateClose 실패");
        }

        checkWriteError();
        out.flush();
    }

    // =========================================================
    // 내부
    // =========================================================

    private void checkWriteError() throws IOException {
        if (writeError != null) {
            Throwable t = writeError;
            writeError = null;
            if (t instanceof IOException) throw (IOException) t;
            throw new IOException("스트리밍 중 오류 발생", t);
        }
    }
}
