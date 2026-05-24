cmake_minimum_required(VERSION 3.20)

# macOS: Xcode 툴체인의 clang++ 을 우선 사용 (CommandLineTools clang은 헤더 경로 문제 있음)
if(APPLE AND NOT CMAKE_CXX_COMPILER)
    execute_process(COMMAND xcrun -f clang++
                    OUTPUT_VARIABLE _xcrun_clangpp
                    OUTPUT_STRIP_TRAILING_WHITESPACE)
    if(_xcrun_clangpp)
        set(CMAKE_CXX_COMPILER "${_xcrun_clangpp}" CACHE STRING "")
    endif()
endif()

project(xcelerate VERSION 0.0.1 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(ZLIB REQUIRED)

add_library(xcelerate SHARED
    src/excel_generator.cpp
)

target_include_directories(xcelerate PUBLIC include)
target_link_libraries(xcelerate PRIVATE ZLIB::ZLIB)

set_target_properties(xcelerate PROPERTIES
    C_VISIBILITY_PRESET hidden
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN ON
)
