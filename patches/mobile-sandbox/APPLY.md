# Patches for mobile-sandbox

Copy these files into mobile-sandbox, replacing the originals.

## Files to copy

```
patches/mobile-sandbox/app/src/main/AndroidManifest.xml       → app/src/main/AndroidManifest.xml
patches/mobile-sandbox/app/src/main/.../ComposeSandboxActivity.kt → app/src/main/.../ComposeSandboxActivity.kt
patches/mobile-sandbox/app/src/main/cpp/CMakeLists.txt         → app/src/main/cpp/CMakeLists.txt
patches/mobile-sandbox/app/src/main/cpp/python.c               → app/src/main/cpp/python.c  (NEW FILE)
```

## What each patch does

1. **AndroidManifest.xml** — Adds FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC,
   POST_NOTIFICATIONS permissions so the terminal stays alive when backgrounded.

2. **ComposeSandboxActivity.kt** — Starts TerminalService as a foreground service
   in onCreate().

3. **CMakeLists.txt** — Adds `python3` executable target that links against
   libpython3.14.so. Produces `libpython3.so` which Android extracts to the
   native lib directory.

4. **python.c** — The Python interpreter source (5 lines). Calls Py_BytesMain().

## After copying, update the submodule

```bash
cd terminal-core
git fetch origin master
git checkout origin/master
cd ..
git add -A
git commit -m "Apply patches: foreground service, python interpreter, permissions"
git push
```

## npm (manual step)

npm needs to be bundled as an asset. Run once:

```bash
curl -fsSL https://registry.npmjs.org/npm/-/npm-10.9.2.tgz | tar xz
mv package/ app/src/main/assets/npm/
```

Then add to the asset extraction in CoryTerminalRuntime or preparePythonAssets
to copy it to `filesDir/python/lib/node_modules/npm/` at runtime.
TerminalBootstrap will automatically create `usr/bin/npm` and `usr/bin/npx`
shell stubs once the npm module directory exists.
