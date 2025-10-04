cmake_minimum_required(VERSION 3.15 FATAL_ERROR)
project(qspbuild)

set(CMAKE_POLICY_DEFAULT_CMP0063 NEW)
set(CMAKE_POLICY_DEFAULT_CMP0077 NEW)

#configure_file(CMakeLists-qsp.txt.in "${CMAKE_BINARY_DIR}/qsp-download/CMakeLists.txt")
#execute_process(COMMAND "${CMAKE_COMMAND}" -G "${CMAKE_GENERATOR}" "-DCMAKE_MAKE_PROGRAM:FILEPATH=${CMAKE_MAKE_PROGRAM}" .
#        WORKING_DIRECTORY "${CMAKE_BINARY_DIR}/qsp-download"
#)
#execute_process(COMMAND "${CMAKE_COMMAND}" --build .
#        WORKING_DIRECTORY "${CMAKE_BINARY_DIR}/qsp-download"
#)
#
#add_subdirectory("${CMAKE_BINARY_DIR}/qsp-src"
#                 "${CMAKE_BINARY_DIR}/qsp-build"
#)

#configure_file(CMakeLists-qslibsnx.txt.in "${CMAKE_BINARY_DIR}/qslibsnx-download/CMakeLists.txt")
#execute_process(COMMAND "${CMAKE_COMMAND}" -G "${CMAKE_GENERATOR}" "-DCMAKE_MAKE_PROGRAM:FILEPATH=${CMAKE_MAKE_PROGRAM}" .
#        WORKING_DIRECTORY "${CMAKE_BINARY_DIR}/qslibsnx-download"
#)
#execute_process(COMMAND "${CMAKE_COMMAND}" --build .
#        WORKING_DIRECTORY "${CMAKE_BINARY_DIR}/qslibsnx-download"
#)
#
#add_subdirectory("${CMAKE_BINARY_DIR}/qslibsnx-src"
#                 "${CMAKE_BINARY_DIR}/qslibsnx-build"
#)

configure_file(CMakeLists-qslibsdh.txt.in "${CMAKE_BINARY_DIR}/qslibsdh-download/CMakeLists.txt")
execute_process(COMMAND "${CMAKE_COMMAND}" -G "${CMAKE_GENERATOR}" "-DCMAKE_MAKE_PROGRAM:FILEPATH=${CMAKE_MAKE_PROGRAM}" .
        WORKING_DIRECTORY "${CMAKE_BINARY_DIR}/qslibsdh-download"
)
execute_process(COMMAND "${CMAKE_COMMAND}" --build .
        WORKING_DIRECTORY "${CMAKE_BINARY_DIR}/qslibsdh-download"
)

add_subdirectory("${CMAKE_BINARY_DIR}/qslibsdh-src"
                 "${CMAKE_BINARY_DIR}/qslibsdh-build"
)