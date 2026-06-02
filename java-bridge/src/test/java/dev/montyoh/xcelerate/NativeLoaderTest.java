package dev.montyoh.xcelerate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeLoaderTest {

    @Test
    @DisplayName("OS 이름을 패키징 규칙에 맞게 정규화한다")
    void normalizeOs() {
        assertEquals("linux", NativeLoader.normalizeOs("Linux"));
        assertEquals("macos", NativeLoader.normalizeOs("Mac OS X"));
        assertEquals("windows", NativeLoader.normalizeOs("Windows 11"));
    }

    @Test
    @DisplayName("아키텍처 이름을 패키징 규칙에 맞게 정규화한다")
    void normalizeArch() {
        assertEquals("x86_64", NativeLoader.normalizeArch("amd64"));
        assertEquals("x86_64", NativeLoader.normalizeArch("x86_64"));
        assertEquals("aarch64", NativeLoader.normalizeArch("arm64"));
        assertEquals("aarch64", NativeLoader.normalizeArch("aarch64"));
    }

    @Test
    @DisplayName("현재 플랫폼 경로를 우선하고 기존 단일 경로를 fallback으로 둔다")
    void resourceCandidates() {
        List<String> candidates = NativeLoader.resourceCandidates("libxcelerate.so");

        assertEquals(2, candidates.size());
        assertEquals("/native/" + NativeLoader.normalizeOs(System.getProperty("os.name")) + "-" +
            NativeLoader.normalizeArch(System.getProperty("os.arch")) + "/libxcelerate.so", candidates.get(0));
        assertEquals("/native/libxcelerate.so", candidates.get(1));
    }
}
