# Server regression tests

Run from the repository root with Python 3 and JDK 11 or newer:

```sh
python3 tests/jvm/run.py
```

Set `JAVA_HOME` when the JDK is not on PATH. The runner uses the cached
`org.json:json:20180813` jar or downloads it from Maven Central. For offline use,
pass `--json-jar /path/to/json-20180813.jar`. Outputs go under `build/jvm-regression`.

The suite compiles the actual server commands, socket workers, filesystem and
SAF adapters, virtual ISO implementation, and settings service with Java 8
language/API checks. Android services, the document provider, preferences, and
logging use small test doubles. Production sources are not transformed.

Coverage includes truncated frames/writes, normal and critical reads, streaming
above 4 MiB, real TCP disconnects, resource ownership, UTF-8 directory records,
root priority and traversal, multipart ISO reads, virtual ISO payloads, aligned
Redump decryption, settings migration, IP filters, and concurrent admission
(100 rounds of 32 clients with limit 1, plus unlimited and limit 3).

These tests do not replace an Android APK build, Android lint, or device testing.
Changing the stub SDK level exercises guarded branches, not the Android runtime.
The fake SAF provider does not model permission revocation or all provider quirks.
The symlink integration test is skipped when the host disallows symlink creation;
it also runs in Linux CI. Validate the APK on API 14, API 21–25, and a recent API,
including network/lifecycle changes and mounting games through a real PS3 client.
