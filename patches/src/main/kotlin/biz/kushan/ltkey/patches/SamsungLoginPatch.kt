package biz.kushan.ltkey.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import biz.kushan.ltkey.patches.shared.Constants.COMPATIBILITY_SMARTTHINGS

/**
 * Opt-in fix for signing in to the patched app on Samsung phones.
 *
 * SmartThings picks its login flow from `mm.e.g()` — "is this a Samsung device". On Samsung it uses
 * the on-device Samsung Account, authenticating with `client_secret = "USING_CLIENT_PACKAGE_INFORMATION"`,
 * i.e. Samsung's server validates the app's *signing certificate*. A Morphe-resigned app has a
 * different certificate, so that path fails ("Samsung account is already logged in on this phone" /
 * "login failed"). On non-Samsung phones the app uses the "Ged" web-OAuth flow at account.samsung.com,
 * which authenticates the user — not the APK signature — and so works for a re-signed app. (This is
 * the exact flow Pixel users already use to sign in, onboard Samsung devices, and read the key.)
 *
 * This forces `mm.e.g()` to return false, so every sign-in site takes the working web path. The app
 * otherwise behaves as its non-Samsung variant, which is the proven-working configuration.
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
        // Return false from mm.e.g() ("is Samsung device") so the sign-in branches choose the Ged
        // web-OAuth path instead of the on-device Samsung Account path that checks the APK signature.
        IsSamsungDeviceFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
    }
}
