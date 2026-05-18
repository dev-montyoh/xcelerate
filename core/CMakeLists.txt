cmake_minimum_required(VERSION 3.20)
# OpenXLSX 의존성인 miniz가 구버전 CMake 정책을 사용하므로 최소 버전을 명시
set(CMAKE_POLICY_VERSION_MINIMUM 3.5)
project(xcelerate VERSION 0.0.1 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# OpenXLSX 다운로드 및 빌드
include(FetchContent)
FetchContent_Declare(
    OpenXLSX
    GIT_REPOSITORY https://github.com/troldal/OpenXLSX.git
    GIT_TAG        master
)
FetchContent_MakeAvailable(OpenXLSX)

add_library(xcelerate SHARED
    src/excel_generator.cpp
)

target_include_directories(xcelerate PUBLIC include)
target_link_libraries(xcelerate PRIVATE OpenXLSX::OpenXLSX)
