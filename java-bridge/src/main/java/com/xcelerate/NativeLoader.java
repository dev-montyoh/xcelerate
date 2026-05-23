package com.xcelerate;

import java.io.*;
import java.nio.file.*;

class NativeLoader {

    private static volatile String resolvedPath = null;

    static synchronized String resolve() {
        if (resolvedPath != null) return resolvedPath;

        String libName = System.mapLibraryName("xcelerate");

        // 1. java.library.path 탐색 (로컬 개발/테스트 모드)
        for (String dir : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
            File f = new File(dir, libName);
            if (f.exists()) {
                resolvedPath = f.getAbsolutePath();
                return resolvedPath;
            }
        }

        // 2. JAR 내부 /native/ 에서 임시 디렉토리로 추출
        String resource = "/native/" + libName;
        try (InputStream in = NativeLoader.class.getResourceAsStream(resource)) {
            if (in != null) {
                String ext = libName.substring(libName.lastIndexOf('.'));
                Path tmp = Files.createTempFile("xcelerate-", ext);
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                tmp.toFile().deleteOnExit();
                resolvedPath = tmp.toAbsolutePath().toString();
                return resolvedPath;
            }
        } catch (IOException e) {
            throw new RuntimeException("네이티브 라이브러리 추출 실패: " + resource, e);
        }

        // 3. JNA 기본 탐색에 위임 (PATH, LD_LIBRARY_PATH 등)
        resolvedPath = "xcelerate";
        return resolvedPath;
    }
}
