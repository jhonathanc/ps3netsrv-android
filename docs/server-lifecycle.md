# Server lifecycle and screen-off operation

The foreground service owns the listener, accepted connections, CPU/Wi-Fi locks
and Ethernet network callback. Recreating or closing an activity does not stop
the service. The screen observes a process-local `LiveData<ServerState>` with
stopped, starting, running and stopping states, using the listener's actual
port/folder snapshot. Running is published only after a successful socket bind.
An occupied port returns the service and UI to stopped and releases its resources.

Repeated start commands leave the existing listener and clients intact. A settings
restart closes the previous listener and clients before starting a replacement on
the service's single executor. An intentional stop releases all sockets, locks,
network callbacks and the foreground notification. `START_STICKY` reconstructs
the listener from saved settings after an OS process restart; existing TCP
connections cannot survive process death. Force-stop, reboot and explicit user
stops do not automatically start the server.

On Android 14+, `connectedDevice` represents the ongoing network interaction with
the PS3. `FOREGROUND_SERVICE_CONNECTED_DEVICE` and `CHANGE_NETWORK_STATE` satisfy
its manifest requirements. This is not a finite cloud synchronization operation.
See [Android foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types).

## Battery settings

CPU and Wi-Fi locks alone do not exempt an app from Doze. On Android 6+, the main
screen offers **Allow background operation** when the app is battery optimized.
The user can grant the system's battery optimization exemption to keep network
access and partial wake locks available during prolonged idle. This is requested
only by tapping that button, since serving a PS3 over a local TCP connection is
the app's core function and cannot use FCM to wake the client. The prompt falls
back to the system battery optimization settings if the direct request is absent.
Notification permission is requested on Android 13+ when starting the server;
denying it does not block server startup.

Keeping the server active uses more battery. Manufacturer-specific battery
restrictions may also require allowing unrestricted background operation in the
device's app settings. Android or the user can still terminate the process.
See [Doze exemptions and testing](https://developer.android.com/training/monitoring-device-state/doze-standby).

## Validation

```sh
python3 tests/jvm/run.py
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.jhonju.ps3netsrv.ServerLifecycleTest
```

The JVM tests include stop before startup, port conflicts, accepted client cleanup,
immediate port reuse and 100 startup/shutdown races. `ServerLifecycleTest` exercises
activity recreation and background/foreground transitions with the same TCP
connection, repeated service starts, settings restarts and recovery from bind
failure. Its temporary server settings are restored after each test.
On API 23+, it also forces Doze with a temporary battery exemption and checks
existing and newly accepted local connections. Battery simulation, idle mode and
the temporary exemption are restored in `finally` blocks. This does not test a
physical Wi-Fi radio or replace a prolonged session with a PS3 client.

For screen locking and prolonged idle, validate on a device with Android 6+:

1. Start the server, grant the battery exemption using the app's button and connect
   a PS3. Rotate the screen and confirm the running status and existing session.
2. Lock the screen; read content through the existing connection and make a new
   connection while the screen remains off.
3. Simulate idle with `adb shell dumpsys battery unplug` and
   `adb shell dumpsys deviceidle force-idle`. Verify both existing and new client
   requests still receive responses. Also test ordinary locked-screen operation
   for a prolonged period on battery, including the device's vendor power manager.
4. Always restore test state with `adb shell dumpsys deviceidle unforce` and
   `adb shell dumpsys battery reset`.
5. Stop from the notification and verify clients disconnect, the notification
   disappears and the app no longer holds its wake/Wi-Fi locks. Reopening the app
   must show stopped. Also deny notification/battery permissions and verify the
   app remains usable without claiming exemption was granted.

Repeat lifecycle checks on API 14, API 21 and a current Android version; emulator
or host tests cannot validate a physical device's Wi-Fi sleep behavior.
