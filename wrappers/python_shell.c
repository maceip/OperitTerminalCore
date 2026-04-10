/*
 * python_shell.c — Minimal Python interpreter wrapper for Android.
 *
 * Build this as a PIE executable that links against libpython3.14.so.
 * Name the output libpython_shell.so so Android extracts it to the
 * native library directory.
 *
 * CMake (add to mobile-sandbox's app/src/main/cpp/CMakeLists.txt):
 *
 *   add_executable(python_shell python_shell.c)
 *   target_link_libraries(python_shell cory_python_runtime)
 *   set_target_properties(python_shell PROPERTIES
 *       OUTPUT_NAME "python_shell"
 *       # Android extracts lib*.so from the APK
 *       PREFIX "lib"
 *       SUFFIX ".so"
 *   )
 *
 * At runtime, TerminalBootstrap symlinks:
 *   nativeLibDir/libpython_shell.so → usr/bin/python3
 *
 * Usage:
 *   python3                    → interactive REPL
 *   python3 script.py          → run a script
 *   python3 -c "print('hi')"  → one-liner
 *   python3 -m pip install x   → run a module
 */
#include <Python.h>

int main(int argc, char *argv[]) {
    return Py_BytesMain(argc, argv);
}
