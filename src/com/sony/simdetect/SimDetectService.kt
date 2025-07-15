/*
 * Copyright (c) 2019-2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Rewritten in Kotlin by Pavel Dubrova <pashadubrova@gmail.com>
 */

package com.sony.simdetect

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.UEventObserver

import com.android.internal.R
import com.android.internal.telephony.uicc.UiccSlot

class SimDetectService : Service() {
    private var TAG = "SimDetectService"
    private var NOTHING_HAPPENED = "0"
    private var SIM_REMOVED = "1"
    private var SIM_INSERTED = "2"
    private val EXTCON_REMOVED = "MECHANICAL=0"
    private val EXTCON_INSERTED = "MECHANICAL=1"

    private val lock = Any()

    private val simDetectEventObserver = object : UEventObserver() {
        override fun onUEvent(event: UEvent) {
            synchronized(lock) {
                val legacyState = event.get("SWITCH_STATE")
                if (legacyState != null) {
                    when (legacyState) {
                        SIM_REMOVED     -> promptForRestart(false)
                        SIM_INSERTED    -> promptForRestart(true)
                    }
                    return
                }
                val extconState = event.get("STATE")
                if (extconState != null) {
                    when (extconState) {
                        EXTCON_REMOVED     -> promptForRestart(false)
                        EXTCON_INSERTED    -> promptForRestart(true)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        val isHotSwapSupported = getResources().getBoolean(R.bool.config_hotswapCapable)
        if (!isHotSwapSupported) {
            simDetectEventObserver.startObserving("SWITCH_NAME=sim_detect")
            simDetectEventObserver.startObserving("NAME=soc:sim_detect")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun promptForRestart(isAdded: Boolean) {
        Handler(Looper.getMainLooper()).post({
            UiccSlot::class.java.getDeclaredMethod("promptForRestart", Boolean::class.java).let {
                it.isAccessible = true
                it.invoke(UiccSlot(this@SimDetectService, false), isAdded)
            }
        })
    }
}
