# Android API 14 and desugaring

The app keeps `minSdkVersion 14` and Java 8 source/bytecode compatibility.
Build it with JDK 11 (JDK 17 also works with Gradle 7.6). The Gradle wrapper
and Android Gradle Plugin remain at 7.6 and 7.4.2.

## Build configuration

`app/build.gradle` enables core library desugaring with
`com.android.tools:desugar_jdk_libs:2.0.4`. This is compatible with AGP 7.4.x;
the 2.1.x library requires AGP 8 or newer. The standard configuration supplies
the collection APIs used here, including `List.sort`, `Map.computeIfAbsent`
and comparator factories/composition. Language desugaring already supports
lambdas, method references, default interface methods and try-with-resources.

Core library desugaring requires multidex when the minimum API is below 21.
The app enables it and extends `MultiDexApplication`, so secondary DEX files
are installed through `attachBaseContext` before `onCreate`. The existing
locale setup is retained. Merely enabling `multiDexEnabled` without installing
multidex at startup would not support API 14–20.

Keep release shrinking enabled: R8 removes unused library implementations.
Desugaring provides compatibility and simpler source code; it does not
automatically improve execution speed. Multidex can add startup work on old
devices, especially with an unshrunk debug APK.

## Changes from the compatibility review

- File adapters share the default `IFile.read` implementation. `IFile` extends
  `Closeable`, allowing scoped ownership through try-with-resources.
- Streams, the SAF cursor and the SFO parser's temporary files close
  automatically, including early returns and failures. Files stored in a
  connection or virtual ISO still use explicit cleanup for their longer lives.
- Virtual ISO sorting shares comparators; the children map uses
  `computeIfAbsent`. Sets track multipart names and visited document IDs.
- Protocol serialization writes into its destination buffer directly, removing
  intermediate arrays and copies while retaining byte order and record sizes.
- Directory size traversal avoids allocating a singleton set for every child,
  closes temporary handles and sums all resolved roots, including file roots.
- Empty-packet detection avoids allocating a zero array for each comparison.
  Path normalization avoids compiling regular expressions for simple trimming.
  Opcode lookup reuses the enum values instead of cloning them per command.
- AndroidX handles foreground-service startup and notification compatibility.
  Only channel creation remains conditional. Error toasts use the main looper
  instead of creating a permanent `HandlerThread` per error.
- The Wi-Fi lock no longer tests for API 12, since the minimum is API 14.
  Folder display preserves configured priority and decodes only SAF URIs,
  retaining literal `+` and `%` characters in filesystem paths.
- ISO identifiers use an explicit locale. File chooser comparisons avoid
  allocating uppercase strings for every comparison.

## Compatibility branches that remain necessary

Desugaring does not backport Android framework APIs. Keep the guards for
document-tree selection (21), persistable URI permissions (19), network
callbacks (21), process network binding (23), notification channels (26) and
locale configuration APIs (17/24). AndroidX can wrap some framework differences
but cannot provide document providers or network callbacks on older Android.

The standard desugaring library used here does not provide
`java.nio.charset.StandardCharsets`. The small cached charset helper is
intentional for API 14–18. `Uri.decode` replaces the screen's direct use of that
class and is available on all supported APIs.

`java.nio.file` is also absent from the standard configuration. The API 26
guards in `StatFileCommand` and `ReadDirEntryCommandV2` retain creation/access
timestamps on supported platforms, with last-modified fallbacks on older
ones. `FileCustom` only needs `java.io.File` metadata, so its former NIO branch
was removed. Do not remove the remaining NIO guards or switch configurations
without checking the selected library's supported Android versions.

The synchronized logger keeps its existing `SimpleDateFormat`: access is
already serialized with file writes, so changing to `java.time` would not
remove that synchronization. Socket transfer, decryption and ISO reads retain
loops and reusable buffers; streams offer no demonstrated benefit there.

## Validation

```sh
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
python3 tests/jvm/run.py
```

The JVM suite checks production server code, including wire-format equality,
SFO resource ownership and directory-size traversal. It runs on a host JVM;
setting a stub SDK to 14 does not execute Dalvik or the packaged desugared code.

Run the instrumented `DesugaringCompatibilityTest` on API 14, API 21 and a recent
API with `./gradlew :app:connectedDebugAndroidTest`. It checks application startup,
collection backports, default interface dispatch, file closure and suppressed
exceptions on API 19+ (the primary exception on earlier versions). Also exercise
folder selection, starting/stopping the service,
notifications, language changes and a real PS3 client on those devices.

References: [Java language and library desugaring](https://developer.android.com/studio/write/java8-support),
[supported APIs](https://developer.android.com/studio/write/java11-default-support-table),
[library version requirements](https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md),
[legacy multidex](https://developer.android.com/build/multidex).
