// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.debug

import android.content.Context
import com.screengarden.app.ui.navigation.DebugDestination

/**
 * Release stub for [DebugCatalog] (P05b).
 *
 * The real implementation lives in `src/debug` and is compiled only into debug
 * builds. Keeping the same signature here means main code can reference the
 * catalogue without a conditional, and a release build can never produce a debug
 * destination: there is no code path in this file that returns a non-null value.
 */
object DebugCatalog {

    /**
     * Always null: release builds have no diagnostic screens.
     *
     * Both parameters are unused by design; the signature must match the debug
     * implementation so main code compiles against either variant.
     */
    @Suppress("UNUSED_PARAMETER")
    fun destination(requested: String?, context: Context): DebugDestination? = null
}
