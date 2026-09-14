cmake_minimum_required(VERSION 3.22)
project(daysay C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)

# Only the CPU backend of ggml is vendored. Keep everything else off.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(WHISPER_CURL OFF CACHE BOOL "" FORCE)

# The DC-1 (MediaTek Helio G99, Cortex-A76/A55) supports ARMv8.2 fp16 arithmetic.
if (ANDROID_ABI STREQUAL "arm64-v8a")
    set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
    add_compile_options(-march=armv8.2-a+fp16+dotprod)
endif ()

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_compile_options(-O3 -ffunction-sections -fdata-sections -fvisibility=hidden)
    add_link_options(-Wl,--gc-sections)
endif ()

add_subdirectory(${WHISPER_DIR} whisper_build EXCLUDE_FROM_ALL)

find_library(LOG_LIB log)

add_library(daysay SHARED jni.c)
target_include_directories(daysay PRIVATE
        ${WHISPER_DIR}/include
        ${WHISPER_DIR}/ggml/include)
target_link_libraries(daysay PRIVATE whisper parakeet ${LOG_LIB} android)
