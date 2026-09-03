# Lintas

**Simple File Transfer App**

Send files instantly with a random code. No accounts, no cloud storage, no hassle.

<p align="center">
  <img src="assets/lintas-icon.png" alt="Lintas app icon" width="200"/>
</p>

---

## Overview

Lintas is a lightweight peer-to-peer style file transfer system. The flow is simple:

1. **Send** a file from the Android app
2. **Get** a short transfer code
3. **Share** the code with your recipient
4. **Receive** the file by entering the code on another device

The system consists of two components: an Android app and a Python backend that handles file storage and code generation.

---

## Architecture

```
┌──────────────┐        ┌──────────────────┐        ┌─────────────────┐
│  Android App │◄──────►│  REST API Backend │◄──────►│  SQLite + Files │
│  (Compose)   │  HTTP  │  (FastAPI)        │        │  (data/uploads) │
└──────────────┘        └──────────────────┘        └─────────────────┘
```

---

## Features

- **Random Transfer Codes** — Unique `xxxxx-xxxxx` codes generated server-side
- **24-Hour Expiration** — Files auto-delete after 24 hours
- **No Accounts** — Fully anonymous, no sign-up required
- **Any File Type** — Send documents, images, videos, archives, etc.
- **Lightweight** — Minimal dependencies, fast to deploy

---

## Backend

### Tech Stack

- Python 3.11
- FastAPI
- SQLite
- Uvicorn

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/upload` | Upload a file, receive a transfer code |
| `GET` | `/api/transfer/{code}` | Get metadata for a transfer (file name, size, type) |
| `GET` | `/api/download/{code}` | Download the actual file |

### How to Run

```bash
cd backend
pip install -r requirements.txt
python -m uvicorn main:app --reload
```

The server runs on **port 8000** by default. Files are stored in `data/uploads/`.

### Base URL

From the host machine the backend is reachable at:

```
http://localhost:8000
```

From an Android **emulator**, use `http://10.0.2.2:8000`. From a **physical device on the same Wi-Fi**, use the computer's LAN IP, e.g. `http://192.168.1.10:8000` (configure this in the app's Server Settings screen).

---

## Android App

### Tech Stack

- Kotlin
- Jetpack Compose
- Material 3 Design
- OkHttp + kotlinx.serialization (HTTP client)

### Server Connection

The app connects to the backend over HTTP. Because the server URL depends on how you run the app, **Lintas lets you change the server URL from the app itself**:

1. Open the app → tap the **gear icon** (top-right) on the Home screen.
2. Enter the backend address and tap **Save**.

The address depends on your setup:

| Setup | Server URL |
|--------|------------|
| Android emulator | `http://10.0.2.2:8000` (default) |
| Physical device, same Wi-Fi | `http://<computer-LAN-IP>:8000` |
| Remote/public server | `http://<your-domain-or-ip>:8000` |

> **Note:** The saved URL is stored on the device (SharedPreferences), so it persists across restarts. The default is `http://10.0.2.2:8000`, which only works for the Android emulator pointing at the host machine. For a physical device, always set the computer's LAN IP instead.

### Configuration

| Property | Value |
|----------|-------|
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

### How to Build

1. Open the project in Android Studio
2. Sync Gradle dependencies
3. Connect a device or start an emulator
4. Click **Run**

### APK Output

The app is built automatically by **GitHub Actions** (see [`.github/workflows/android.yml`](.github/workflows/android.yml)) on every push to `main`. Two APKs are produced as CI artifacts:

- **Release APK** — optimized (R8 minify + resource shrinking on), signed with a debug key so it is directly installable. Build locally with:
  ```
  ./gradlew assembleRelease
  ```
- **Debug APK** — full-featured debug build at:
  ```
  app/build/outputs/apk/debug/app-debug.apk
  ```

To distribute an installable APK to other users, create a `v*` git tag (e.g. `v1.0.0`). The CI workflow will build the release APK and attach it to a **GitHub Release**, which anyone can download from the repository's *Releases* page without a GitHub login.

---

## Transfer Flow

```
Sender                          Backend                        Receiver
  │                               │                               │
  │  1. POST /api/upload          │                               │
  │  ────────────────────────────►│                               │
  │                               │  Store file                   │
  │                               │  Generate code                │
  │  ◄────────────────────────────│                               │
  │  2. Receive code              │                               │
  │                               │                               │
  │  ──── Share code ────────────────────────────────────────────►│
  │                               │                               │
  │                               │  3. GET /api/transfer/{code}  │
  │                               │  ◄────────────────────────────│
  │                               │  4. Return file metadata      │
  │                               │  ────────────────────────────►│
  │                               │                               │
  │                               │  5. GET /api/download/{code}  │
  │                               │  ◄────────────────────────────│
  │                               │  6. Return file data          │
  │                               │  ────────────────────────────►│
```

---

## Random Code System

Transfer codes follow the format `xxxxx-xxxxx`:

- **5 characters - 5 characters**, separated by a hyphen
- Alphanumeric characters (`a-z`, `A-Z`, `0-9`)
- **Case-sensitive** — `AbC12-xYz34` is different from `abc12-xyz34`
- Generated **server-side** using Python's `secrets` module for cryptographic randomness
- Codes are not sequential or predictable

---

## 24-Hour Expiration

- Every uploaded file is assigned a timestamp
- The server checks expiration on every access
- Files older than **24 hours** are automatically rejected
- A background cleanup process removes expired files from disk
- Expiration is enforced **server-side** — the client cannot bypass it

---

## Security

- **Predictable Code Prevention** — Codes are generated using `secrets`, not `random`, ensuring cryptographic randomness
- **Server-Side Validation** — All file operations are validated on the backend before serving data
- **No Sensitive Data Exposure** — Transfer codes are the only identifier; no user data is collected or stored
- **File Type Safety** — Files are stored with generated names, preventing path traversal attacks
- **Expiration Enforcement** — Expired files are inaccessible regardless of having the correct code

---

## Project Structure

```
lintas/
├── .github/
│   └── workflows/
│       ├── android.yml          # Android CI workflow
│       └── backend.yml          # Backend CI workflow
├── backend/
│   ├── data/
│   │   └── uploads/             # Uploaded files storage
│   ├── main.py                  # FastAPI application
│   └── requirements.txt         # Python dependencies
├── app/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/lintas/
│   │               └── MainActivity.kt
│   ├── build.gradle.kts
│   └── src/
│       └── androidTest/
│       └── test/
├── build.gradle.kts             # Root Gradle build file
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── README.md
```

---

## Technologies Used

| Component | Technology |
|-----------|------------|
| Android UI | Kotlin, Jetpack Compose, Material 3 |
| HTTP Client | Retrofit |
| Backend Framework | FastAPI |
| Runtime | Python 3.11, Uvicorn |
| Database | SQLite |
| File Storage | Local filesystem |
| CI/CD | GitHub Actions |
| Code Generation | Python `secrets` module |

---

## Known Limitations

- **Max File Size: 500MB** — Large files may cause memory issues on constrained devices
- **SQLite** — Not suitable for high-concurrency deployments; consider PostgreSQL for production scale
- **Single-Server Deployment** — No built-in load balancing or multi-server support
- **No Encryption at Rest** — Files are stored unencrypted on the server disk
- **No Authentication** — Anyone with a valid code can download the file
