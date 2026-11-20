# LocalThings Owner PSK — Morphe patch

Add this repo as a source in **Morphe Manager** — no sideloading:

<a href="https://morphe.software/add-source?github=neoKushan/smortthings">
  <img src=".github/assets/add-to-morphe.svg" alt="Add to Morphe" height="48">
</a>

Or add the URL manually in Morphe Manager: `https://github.com/neoKushan/smortthings`

A **[Morphe](https://morphe.software) patch** for the official Samsung **SmartThings / OneConnect**
Android app. After you pair a supported device, it shows that device's **OwnerPSK** (the credential
[Home Assistant LocalThings](https://github.com/mbillow/localthings) needs for local control) on the
device's **Edit** screen, each value tap-to-copy. The key is extracted **on-device, reusing the
app's own key store** — nothing is sent anywhere, and no root is required.

> **Proof of concept.** Developed against **SmartThings 1.8.52.23** and tested on a single
> **Samsung HW-Q950A** soundbar. It may not work on other models, firmware, or app versions. The
> repository is provided as-is.

## What this is (and is not)

- It is a **patch**, distributed as a Morphe patch bundle (`.mpp`). You apply it yourself, with
  Morphe Manager, to your own copy of the SmartThings app. **No modified APK is distributed.**
- It is **not** a credential dumper you run with root, and it is **not** a cloud tool. It runs
  inside the app you already use, reads the key the app already holds for *your* device, and shows
  it to you.

## Install & use (step by step)

**You need:** an Android phone, the **Morphe Manager** app, and the **SmartThings `1.8.52.23`**
APK. The patch is pinned to that exact version — a newer SmartThings will not patch.

### 1. Install Morphe Manager

Get the Morphe Manager app from **[morphe.software](https://morphe.software)** and install it
(you may need to allow "install unknown apps" for your browser/file manager).

### 2. Add this repo as a patch source

**Recommended — no sideloading:** tap **[Add to Morphe](https://morphe.software/add-source?github=neoKushan/smortthings)**
above, or in Morphe Manager open **Sources → Add** and paste
`https://github.com/neoKushan/smortthings`. Manager fetches the latest release itself and keeps it
updated. It appears as **"Smortthings Patches"** with one patch,
**"LocalThings Owner PSK in device settings"**.

*Alternative (offline):* download **`patches-*.mpp`** from [Releases](../../releases) and import the
file in Morphe Manager instead.

### 3. Patch SmartThings

1. In Manager, choose to patch an app and select **SmartThings** (`com.samsung.android.oneconnect`)
   at version **`1.8.52.23`**. Manager can download this version for you, or you can supply the APK.
2. Make sure the **"LocalThings Owner PSK…"** patch is ticked, then **Patch** and **Install**.
   - If a different-signed SmartThings is already installed, Manager will offer to reinstall it;
     your SmartThings login and paired devices are preserved across a same-source update.

### 4. Read a device's Owner PSK

1. Open the **patched SmartThings**, make sure your device is **paired and shown**.
2. Tap the device → its **⋮ / Settings → Edit** screen.
3. Scroll to the **LocalThings** section:
   - **Owner PSK** — tap to copy. This is what LocalThings needs.
   - **Identity** — your account's owner UUID (same for all your devices); tap to copy.
4. In Home Assistant **[LocalThings](https://github.com/mbillow/localthings)**, add the device with
   its **IP address** and paste the **Owner PSK**.

> **Which devices show a key?** Only devices that were **locally onboarded with an OwnerPSK** (an
> OCF `credtype:1` record in the app's store) will show one. A device with no local key shows
> *"No local key for this device yet."* — that is expected, not a bug; it means the app holds no
> local OwnerPSK for it.

## Patches

<!-- PATCHES_START -->
> **[v1.0.1](https://github.com/neoKushan/smortthings/releases/tag/v1.0.1)**&nbsp;&nbsp;&middot;&nbsp;&nbsp;`main`&nbsp;&nbsp;&middot;&nbsp;&nbsp;**1 patches** across **1 apps**&nbsp;&nbsp;&middot;&nbsp;&nbsp;[Full details](PATCHES.md)

| # | App | Patches | Version | Package |
|---|---|---|---|---|
| 1 | [**SmartThings**](PATCHES.md#smartthings-comsamsungandroidoneconnect) | 1 | `1.8.52.23` | [`com.samsung.android.oneconnect`](https://play.google.com/store/apps/details?id=com.samsung.android.oneconnect) |
<!-- PATCHES_END -->

## How it works

1. **Capture** — on the Edit screen, the extension unwraps the IoTivity storage key via the app's
   own `CryptoManager`, decrypts the OCF credential store (`files/*.datenc`:
   `IV(16) | AES-256-CBC | HMAC-SHA256(32)`, one key for both, custom length trailer), and parses
   the **indefinite-length** CBOR for `credtype == 1` OwnerPSK records.
2. **Store** — recovered `{di → identity, pskHex}` is kept in the extension's own private prefs,
   keyed by the OCF `di`.
3. **Display** — a "LocalThings Owner PSK" section (Owner PSK + Identity, tap-to-copy + toast) is
   appended to `EditDeviceActivity`. The device being edited is matched to its credential by
   equality of the SmartThings cloud `deviceId` and the OCF `di`; with no exact match, nothing is
   shown (a device never borrows another device's key).

See [`PATCH-SCOPE.md`](PATCH-SCOPE.md) for the full reverse-engineering notes and the confirmed
extraction chain.

## Layout

```
extensions/extension/src/main/java/biz/kushan/ltkey/   the injected code
  DtCbor.java         minimal CBOR decoder (dependency-free)
  DtStoreCrypto.java  AES-256-CBC + HMAC-SHA256 store decrypt (matches native psiDecrypt)
  DtCredParser.java   CBOR → OwnerPSK records, heavily guarded
  DtCred.java         data holder
  DtHarvest.java      orchestrates unwrap + decrypt + parse + store
  DtDi.java           resolves the app's CryptoManager (version-pinned reflection)
  DtStore.java        our private SharedPreferences
  DtEditRow.java      appends the section to EditDeviceActivity
  DtEntry.java        single entry point the patch calls
  DtLog.java          logging
patches/src/main/kotlin/biz/kushan/ltkey/patches/      the Morphe patch
  Fingerprints.kt     EditDeviceActivity.onCreate
  EditDevicePatch.kt  injects one static call into onCreate
test/DtSelfTest.java  standalone JDK self-test for the extraction core (no device needed)
```

## Extraction core is unit-tested off-device

The crypto + CBOR logic is Android-free and proven with synthetic vectors before it ever runs in
the app:

```bash
JDK=/path/to/jdk17
"$JDK/bin/javac" -d out \
  extensions/extension/src/main/java/biz/kushan/ltkey/DtCbor.java \
  extensions/extension/src/main/java/biz/kushan/ltkey/DtCred.java \
  extensions/extension/src/main/java/biz/kushan/ltkey/DtStoreCrypto.java \
  extensions/extension/src/main/java/biz/kushan/ltkey/DtCredParser.java \
  test/DtSelfTest.java
"$JDK/bin/java" -cp out biz.kushan.ltkey.DtSelfTest
# 16 passed, 0 failed  (round-trip, nested + indefinite-length CBOR, 16/32-byte PSK, multi-cred, and every guard path)
```

## Building the patch bundle

This repo holds the patch + extension **sources**; the Morphe/Gradle wrapper comes from the
[`MorpheApp/morphe-patches-template`](https://github.com/MorpheApp/morphe-patches-template). To
build:

1. Create a repo from that template and drop these `patches/` and `extensions/` sources in
   (adjust the group in `patches/build.gradle.kts`).
2. `./gradlew buildAndroid` → `patches/build/libs/patches-*.mpp`.
3. Apply to a SmartThings 1.8.52.23 APK with **Morphe Desktop** / Morphe Manager, install, open a
   paired device's **Edit** screen.

## Safety / design

- **Never throws into the host.** Every extension path is guarded; on any failure the Edit screen
  looks exactly like stock.
- **Only shows a key matched to the exact device** (cloud `deviceId` == OCF `di`) from an
  HMAC-verified store, well-formed CBOR, 16/32-byte PSK, non-zero identity. Prefers showing
  nothing over showing a wrong value.
- **Version-pinned** to 1.8.52.23; fingerprints fail-closed on other builds.
- Tested on one device only — the guards exist because other device classes are unvalidated.

## License

GPL-3.0 (as required for Morphe patches).
