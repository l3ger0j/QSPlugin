cmake_minimum_required(VERSION 3.15 FATAL_ERROR)
project(qspbuild)

include(FetchContent)

FetchContent_Declare(
        qsp-external
        GIT_REPOSITORY https://github.com/QSPFoundation/qsp.git
        GIT_TAG master
        GIT_PROGRESS true
        SUBBUILD_DIR "${CMAKE_CURRENT_BINARY_DIR}/qsp-download"
        SOURCE_DIR "${CMAKE_CURRENT_BINARY_DIR}/qsp-src"
        BINARY_DIR "${CMAKE_CURRENT_BINARY_DIR}/qsp-build"
)
FetchContent_MakeAvailable(qsp-external)

FetchContent_Declare(qslibsnx-external
        GIT_REPOSITORY https://github.com/l3ger0j/QSLibSNX.git
        GIT_TAG master
        GIT_PROGRESS true
        SUBBUILD_DIR "${CMAKE_CURRENT_BINARY_DIR}/qslibsnx-download"
        SOURCE_DIR "${CMAKE_CURRENT_BINARY_DIR}/qslibsnx-src"
        BINARY_DIR "${CMAKE_CURRENT_BINARY_DIR}/qslibsnx-build"
)
FetchContent_MakeAvailable(qslibsnx-external)

FetchContent_Declare(qslibsdh-external
        GIT_REPOSITORY https://github.com/l3ger0j/QSLibSDH.git
        GIT_TAG master
        GIT_PROGRESS true
        SUBBUILD_DIR "${CMAKE_CURRENT_BINARY_DIR}/qslibsdh-download"
        SOURCE_DIR "${CMAKE_CURRENT_BINARY_DIR}/qslibsdh-src"
        BINARY_DIR "${CMAKE_CURRENT_BINARY_DIR}/qslibsdh-build"
)
FetchContent_MakeAvailable(qslibsdh-external)