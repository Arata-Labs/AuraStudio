cmake_minimum_required(VERSION 3.22.1)

project("termux" C)

add_library(termux SHARED termux.c)
target_link_libraries(termux log)