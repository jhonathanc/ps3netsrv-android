# PS3NetSrv for Android
![Android CI](https://github.com/jhonathanc/ps3netsrv-android/actions/workflows/gradle.yml/badge.svg)

This is an Android implementation of the `ps3netsrv` server, allowing you to stream ISOs and other media files to your PlayStation 3 directly from your Android device.

## Features

- **Serve PS3 ISOs/Games:** Stream your backups over the local network to your console (WebMAN MOD / multiMAN compatible).
- **Multiple Folders:** Support for serving files from multiple directories on your device.
- **Ordered Priority:** If a file exists in multiple folders, the one in the first listed folder takes priority.
- **Port Configuration:** Customizable server port (Default: 38008).
- **Connection Limits:** Set a maximum number of connected clients.
- **Security:**
  - **IP Whitelist/Blacklist:** Restrict access to specific IP addresses.
  - **Read-Only Mode:** Prevent clients from deleting or modifying files on your device.
- **Persistent Service:** Option to keep the server running in the background with a persistent notification (Android 13+ supported).
- **Multilingual:** Available in English, Portuguese, Spanish, and Italian.

## How to Use

1.  **Grant Permissions:** Upon first launch, grant the necessary storage permissions so the app can access your game files. On Android 13+, also grant notification permissions to ensure the service runs reliably in the background.
2.  **Add Folders:**
    - Tap "Add folder" (or "Adicionar pasta").
    - Navigate to the directory containing your PS3 content (e.g., `GAMES`, `PS3ISO`, `DVDISO`).
    - You can add multiple folders. The order matters for file priority.
3.  **Configure (Optional):**
    - Tap the **Settings** (Configurações) icon in the menu.
    - **Port:** Change the listening port if needed.
    - **IP Filtering:** Set up a whitelist/blacklist if you want to secure access.
    - **Max Connections:** Limit how many devices can connect simultaneously.
4.  **Start Server:**
    - Return to the main screen.
    - Tap **Start Server** (Iniciar servidor).
    - The status will update to show the running port and IP address.
5.  **Connect from PS3:**
    - On your PS3 (running WebMAN MOD or multiMAN), configure the `ps3netsrv` settings to point to your Android device's IP address and the port you configured (default 38008).

## About

- **Developer:** Jhonathan Corrêa
- **Thanks to:** Aldostools (for the original concept and tools)

## Disclaimer

This application is intended for legal use with your own personal backups. The developer is not responsible for any misuse.
