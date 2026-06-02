package dev.montyoh.xcelerate;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;

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

        // 2. JAR 내부 /native/ 에서 현재 OS/ARCH에 맞는 바이너리를 임시 디렉토리로 추출
        for (String resource : resourceCandidates(libName)) {
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
        }

        // 3. JNA 기본 탐색에 위임 (PATH, LD_LIBRARY_PATH 등)
        resolvedPath = "xcelerate";
        return resolvedPath;
    }

    static List<String> resourceCandidates(String libName) {
        String platform = normalizeOs(System.getProperty("os.name")) + "-" + normalizeArch(System.getProperty("os.arch"));
        return List.of(
            "/native/" + platform + "/" + libName,
            "/native/" + libName
        );
    }

    static String normalizeOs(String osName) {
        String normalized = osName.toLowerCase(Locale.ROOT);
        if (normalized.contains("mac") || normalized.contains("darwin")) return "macos";
        if (normalized.contains("win")) return "windows";
        if (normalized.contains("nux") || normalized.contains("linux")) return "linux";
        return normalized.replaceAll("[^a-z0-9]+", "");
    }

    static String normalizeArch(String arch) {
        String normalized = arch.toLowerCase(Locale.ROOT);
        if (normalized.equals("x86_64") || normalized.equals("amd64")) return "x86_64";
        if (normalized.equals("aarch64") || normalized.equals("arm64")) return "aarch64";
        return normalized.replaceAll("[^a-z0-9_]+", "");
    }
}
