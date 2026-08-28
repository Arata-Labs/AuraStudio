cmake_minimum_required(VERSION 3.22.1)

project("termux-bootstrap" C ASM)

# Generate termux-bootstrap-zip.S from the template. Unlike a plain
# configure_file, this custom command also DEPENDS on the bootstrap zip
# archives, so swapping in a new bootstrap-*.zip (same filename, new binary
# content) re-renders the .S and recompiles it — no `gradle clean` needed.
file(GLOB BOOTSTRAP_ZIPS "${CMAKE_CURRENT_SOURCE_DIR}/bootstrap-*.zip")

add_custom_command(
  OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/termux-bootstrap-zip.S
  COMMAND ${CMAKE_COMMAND}
          "-DSRC=${CMAKE_CURRENT_SOURCE_DIR}"
          "-DBIN=${CMAKE_CURRENT_BINARY_DIR}"
          -P ${CMAKE_CURRENT_SOURCE_DIR}/generate-bootstrap-zip.cmake
  DEPENDS
    ${CMAKE_CURRENT_SOURCE_DIR}/termux-bootstrap-zip.S.in
    ${BOOTSTRAP_ZIPS}
  COMMENT "Generating termux-bootstrap-zip.S (embeds bootstrap archive)"
  VERBATIM
)

add_library(termux-bootstrap SHARED termux-bootstrap.c ${CMAKE_CURRENT_BINARY_DIR}/termux-bootstrap-zip.S)
target_include_directories(termux-bootstrap PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
target_link_libraries(termux-bootstrap log)