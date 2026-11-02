// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.di

import android.content.Context
import com.screengarden.app.data.local.ScreenGardenDatabase

/**
 * Manual dependency injection root, created once by [com.screengarden.app.ScreenGardenApplication].
 *
 * DECISION: manual DI instead of Hilt.
 * - No kapt step (only KSP for Room, which we need anyway), so the toolchain
 *   stays simple.
 * - The app has one Activity and no dynamic feature modules, so a graph library
 *   buys nothing yet.
 * - Repositories and use cases are added here as plain `val`s once their
 *   implementations land (Task 1.1 defines only interfaces, so nothing is
 *   constructed for them yet - see Section 11 of AGENT_MEMORY).
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /**
     * The Room database. Lazy so a cold start does not touch disk until a screen
     * actually asks for data.
     */
    val database: ScreenGardenDatabase by lazy { ScreenGardenDatabase.build(appContext) }
}
