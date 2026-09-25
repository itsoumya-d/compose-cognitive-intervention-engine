# CMakeLists.txt for tun2socks stub library
#
# This builds a STUB implementation that allows the app to compile and run.
# For actual VPN functionality, replace with real hev-socks5-tunnel library.

cmake_minimum_required(VERSION 3.10.2)

project("tun2socks-stub")

# Create shared library from stub implementation
add_library(
        tun2socks-jni
        SHARED
        tun2socks-stub.c
)

# Link with Android log library
find_library(log-lib log)
target_link_libraries(
        tun2socks-jni
        ${log-lib}
)
