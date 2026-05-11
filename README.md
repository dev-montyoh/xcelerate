# Xcelerate

> C++과 JNA 기반의 고성능 Excel 생성 라이브러리

Java/Spring 환경에서 수백만 건의 데이터를 빠르게 Excel 파일로 생성할 수 있습니다.  
Apache POI 기반의 기존 방식 대비 획기적으로 빠른 처리 속도를 제공합니다.

---

## 특징

- **고성능** — C++ 네이티브 코어 기반으로 수백만 행을 빠르게 처리
- **JVM 친화적** — JNA 브릿지를 통해 Java/Kotlin/Spring에서 의존성 하나로 사용
- **경량** — 별도 서버 없이 라이브러리 형태로 바로 사용 가능
- **안전** — C++ 크래시가 JVM에 영향을 주지 않도록 설계

---

## 설치

> 현재 개발 중입니다. Maven Central 배포 예정입니다.

```groovy
// build.gradle
dependencies {
    implementation 'com.xcelerate:xcelerate:0.0.1'
}
```

---

## 프로젝트 구조

```
xcelerate/
 ├── core/           # C++ 네이티브 코어 (OpenXLSX 기반)
 └── java-bridge/    # JNA 브릿지 및 Java API
```

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| 네이티브 코어 | C++17, OpenXLSX, CMake |
| JVM 브릿지 | Java 17, JNA 5.14 |
| 빌드 | Gradle, CMake |

---

## 라이선스

MIT License © 2026 Xcelerate

---

개발 참고사항은 [CONTRIBUTING.md](./CONTRIBUTING.md)를 확인해주세요.
