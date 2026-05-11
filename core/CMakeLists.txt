cmake_minimum_required(VERSION 3.20)
project(xcelerate VERSION 0.0.1 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(xcelerate SHARED
    src/excel_generator.cpp
)

target_include_directories(xcelerate PUBLIC include)
