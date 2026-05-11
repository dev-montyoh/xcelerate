# 개발 참고사항

## 커밋 메시지 규칙 (Conventional Commits)

커밋 메시지는 아래 형식을 따릅니다.

```
<타입>: <제목>

<본문 (선택)>
```

| 타입 | 설명 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `chore` | 빌드 설정, 패키지 관리 등 기능과 무관한 작업 |
| `refactor` | 기능 변경 없이 코드 구조 개선 |
| `docs` | 문서 수정 |
| `test` | 테스트 코드 추가 및 수정 |
| `style` | 포맷, 공백 등 코드 스타일만 변경 |

---

## 브랜치 전략

```
main          # 안정된 릴리즈 브랜치
└── feature/  # 기능 개발 브랜치 (예: feature/excel-core)
└── fix/      # 버그 수정 브랜치 (예: fix/crash-on-large-data)
```

---

## 프로젝트 구조

```
xcelerate/
 ├── core/               # C++ 네이티브 코어
 │    ├── CMakeLists.txt
 │    ├── include/       # 헤더 파일
 │    └── src/           # 구현 파일
 └── java-bridge/        # JNA 브릿지 및 Java API
      └── src/
           ├── main/java/com/xcelerate/
           │    ├── ExcelLib.java       # JNA 브릿지
           │    └── ExcelGenerator.java # 외부 공개 API
           └── test/java/com/xcelerate/
```

---

## 버전 관리

버전은 루트의 `gradle.properties`에서 관리합니다.

```properties
version=0.0.1
group=com.xcelerate
```

버전은 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

```
MAJOR.MINOR.PATCH
  │     │     └── 버그 수정
  │     └──────── 하위 호환 기능 추가
  └────────────── 하위 호환 불가능한 변경
```

---

## 빌드 방법

**Java 브릿지 빌드**
```bash
./gradlew :java-bridge:build
```

**C++ 코어 빌드**
```bash
cd core
cmake -B build
cmake --build build
```
