package biz.kushan.ltkey.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import biz.kushan.ltkey.patches.shared.Constants.COMPATIBILITY_SMARTTHINGS

/**
 * Opt-in fix for signing in to the patched app on Samsung phones. Takes two bytecode nudges, both
 * on un-ambiguous targets that fail closed on other builds:
 *
 * 1. `mm.e.g()` ("is this a Samsung device") → false. SmartThings picks its login flow from this:
 *    true routes to the on-device Samsung Account (which validates the APK signing certificate and so
 *    rejects a Morphe-resigned app), false routes to the "Ged" web flow at account.samsung.com that
 *    authenticates the *user*, not the signature. Forcing false makes a Samsung phone take the web
 *    path — the exact flow Pixel users already use to sign in, onboard, and read the key.
 *
 * 2. `com.osp.app.signin.sasdk.common.Util.isStandAloneSaAppInstalled(Context)` → false. Within that
 *    web flow, the bundled Samsung Account SDK still hands off to the Samsung Account app's
 *    `WebSdkActivity` when it is installed (every Galaxy) — and that activity signature-checks the
 *    caller and finishes ("Calling package has not available signature"). Forcing this false makes
 *    the SDK open the browser Custom Tab instead, the same branch non-Samsung devices use, which
 *    authenticates the user and completes.
 *
 * Off by default, and only useful together with "Allow install on Samsung phones". Experimental:
 * developed without a Samsung device to test on.
 */
@Suppress("unused")
val samsungLoginPatch = patch(
    name = "Use web login on Samsung phones",
    description = "Forces the non-Samsung (web) Samsung Account login so the re-signed app can sign " +
        "in on a Samsung phone. Enable it alongside \"Allow install on Samsung phones\" if login " +
        "fails with \"Samsung account is already logged in\" / \"login failed\". Unnecessary on " +
        "non-Samsung phones.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SMARTTHINGS)

    execute {
        // (1) mm.e.g() ("is Samsung device") → false, so sign-in takes the Ged web path, not native.
        IsSamsungDeviceFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
        // (2) isStandAloneSaAppInstalled() → false, so the Samsung Account SDK opens the browser
        // Custom Tab instead of the Samsung Account app's signature-checking WebSdkActivity.
        SaSdkStandAloneAppFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
    }
}
