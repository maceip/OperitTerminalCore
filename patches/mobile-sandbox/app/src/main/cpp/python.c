/*
 * Python interpreter for Android.
 * Links against libpython3.14.so, calls Py_BytesMain().
 * Built as libpython3.so so Android extracts it to the native lib dir.
 */
#include <Python.h>

int main(int argc, char *argv[]) {
    return Py_BytesMain(argc, argv);
}
