# 개발 가이드

## 사전 요구사항

- Java 17 이상
- Gradle 8 이상
- CMake 3.14 이상
- C++17 호환 컴파일러 (GCC / Clang / MSVC)
- zlib (`brew install zlib` / `apt install zlib1g-dev`)

## 로컬 환경에서 빌드

1. **C++ 코어 빌드**
   ```bash
   cmake -S core -B core/build -DCMAKE_BUILD_TYPE=Release
   cmake --build core/build
   ```

2. **Java 브릿지 빌드**
   ```bash
   ./gradlew :java-bridge:build
   ```

3. **로컬 Maven 배포**
   ```bash
   ./gradlew :java-bridge:publishToMavenLocal
   ```

## Git 브랜치 전략

- **`main`**: 배포된 버전만 존재. 직접 커밋 금지
- **`develop`**: 개발 통합 브랜치
- **`feature/*`**: 기능 개발. `develop`에서 분기 후 Pull Request로 Merge (예: `feature/csv-export`)
- **`release/*`**: 배포 준비. `develop`에서 분기 → `main` + `develop`으로 Merge (예: `release/0.1.0`)
- **`hotfix/*`**: 운영 긴급 수정. `main`에서 분기 → `main` + `develop`으로 Merge (예: `hotfix/0.0.2`)

## 커밋 메시지 규칙

[Conventional Commits](https://www.conventionalcommits.org/) 규칙을 따릅니다.

```
<type>: <subject>
```

### Type

| type | 설명 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없이 코드 구조 개선 |
| `chore` | 빌드 설정, 패키지 관리 등 기능과 무관한 작업 |
| `docs` | 문서 수정 |
| `test` | 테스트 코드 추가 및 수정 |
| `style` | 포맷, 공백 등 코드 스타일만 변경 |

### 예시

```
feat: CSV 스트리밍 지원 추가
fix: 다중 시트 분리 시 헤더 누락 버그 수정
refactor: NativeLoader 플랫폼 감지 로직 개선
docs: Spring Boot 통합 예제 추가
```

## 버전 관리

버전은 `java-bridge/build.gradle`에서 관리하며 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

```groovy
groupId    = 'dev.montyoh'
artifactId = 'xcelerate'
version    = '0.0.2'
```

```
MAJOR.MINOR.PATCH
  │     │     └── 버그 수정
  │     └──────── 하위 호환 기능 추가
  └────────────── 하위 호환 불가능한 변경
```
