/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.di

import android.content.Context
import dev.shadow.eclipselauncher.core.common.EpochClock
import dev.shadow.eclipselauncher.core.common.SystemEpochClock
import dev.shadow.eclipselauncher.core.model.InstanceSummary

/**
 * Manual dependency container. The app is deliberately not using a service
 * locator or an annotation processor: every dependency is visible here, and a
 * module that needs something new has to say so in its constructor.
 *
 * This first shell has no database or network. The instance list is empty and
 * stays empty until the real repository exists; a fake instance would be worse
 * than an empty screen.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val clock: EpochClock = SystemEpochClock

    val instances: List<InstanceSummary> = emptyList()
}
