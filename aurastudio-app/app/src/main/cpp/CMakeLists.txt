cmake_minimum_required(VERSION 3.22.1)

project("aurastudio" C)

add_library(aurastudio SHARED termux.c)

target_link_libraries(aurastudio log)
