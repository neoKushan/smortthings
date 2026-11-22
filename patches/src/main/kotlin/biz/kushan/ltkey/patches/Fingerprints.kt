package biz.kushan.ltkey.patches

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * `EditDeviceActivity.onCreate(Bundle)` — our single injection site. The package
 * `com.samsung.android.oneconnect.editdevice.view` is not obfuscated in 1.8.52.23, so the full
 * class matches directly. If a future build renames/moves it, this fingerprint fails cleanly and
 * the patch simply does not apply (no mis-patch).
 */
object EditDeviceOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/samsung/android/oneconnect/editdevice/view/EditDeviceActivity;",
    name = "onCreate",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

/**
 * `mm.e.g()` in 1.8.52.23 — the app's "is this a Samsung device" gate
 * (`"samsung".equals(Build.MANUFACTURER) && !MODEL.contains("Nexus")`, cached in a static int).
 * Every sign-in site branches on it: true → the on-device Samsung Account path (which validates the
 * APK signature and so fails for a re-signed app), false → the "Ged" web-OAuth path that works on
 * non-Samsung phones. A sibling class has an identical check, so we pin the exact obfuscated
 * class+name (plus its two string constants) to hit only this one; it fails closed on other builds.
 */
object IsSamsungDeviceFingerprint : Fingerprint(
    definingClass = "Lmm/e;",
    name = "g",
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("samsung", "Nexus"),
)
