Copy these files into mobile-sandbox, replacing the originals:

  patches/mobile-sandbox/app/src/main/AndroidManifest.xml
  patches/mobile-sandbox/app/src/main/java/com/example/orderfiledemo/compose/ComposeSandboxActivity.kt

Then update the submodule:

  cd mobile-sandbox/terminal-core
  git fetch origin master
  git checkout 8601c2a
  cd ..
  git add -A && git commit -m "Wire foreground Service, update terminal-core"
  git push
