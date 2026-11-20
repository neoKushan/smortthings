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
