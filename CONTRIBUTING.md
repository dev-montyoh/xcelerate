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

## 브랜치 전략 (Git Flow)

```
main
 └─ develop
      ├─ feature/*   # 기능 개발
      ├─ release/*   # 배포 준비
      └─ hotfix/*    # 긴급 버그 수정
```

### 브랜치별 규칙

| 브랜치 | 분기 기준 | 머지 대상 | 설명 |
|--------|----------|----------|------|
| `main` | — | — | 배포된 버전만 존재. 직접 커밋 금지 |
| `develop` | `main` | — | 개발 통합 브랜치 |
| `feature/*` | `develop` | `develop` | 기능 개발 (예: `feature/csv-export`) |
| `release/*` | `develop` | `main` + `develop` | 배포 준비, 버전 수정 (예: `release/0.1.0`) |
| `hotfix/*` | `main` | `main` + `develop` | 운영 긴급 수정 (예: `hotfix/0.0.2`) |

### 기능 개발 흐름

```bash
git checkout develop
git checkout -b feature/my-feature

# 작업 후
git checkout develop
git merge feature/my-feature
git branch -d feature/my-feature
```

### 릴리즈 흐름

```bash
git checkout develop
git checkout -b release/0.1.0

# 버전 수정, 최종 테스트 후
git checkout main
git merge release/0.1.0
git tag v0.1.0

git checkout develop
git merge release/0.1.0
git branch -d release/0.1.0
```

### 핫픽스 흐름

```bash
git checkout main
git checkout -b hotfix/0.0.2

# 수정 후
git checkout main
git merge hotfix/0.0.2
git tag v0.0.2

git checkout develop
git merge hotfix/0.0.2
git branch -d hotfix/0.0.2
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
           ├── main/java/dev/montyoh/xcelerate/
           │    ├── ExcelLib.java    # JNA 브릿지
           │    ├── ExcelWriter.java # 외부 공개 API
           │    ├── FileType.java    # EXCEL / CSV 타입
           │    └── NativeLoader.java
           └── test/java/dev/montyoh/xcelerate/
```

---

## 버전 관리

버전은 `java-bridge/build.gradle`에서 관리합니다.

```groovy
groupId    = 'dev.montyoh'
artifactId = 'xcelerate'
version    = '0.0.1'
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
cmake -S core -B core/build -DCMAKE_BUILD_TYPE=Release
cmake --build core/build
```

**로컬 Maven 배포**
```bash
./gradlew :java-bridge:publishToMavenLocal
```
