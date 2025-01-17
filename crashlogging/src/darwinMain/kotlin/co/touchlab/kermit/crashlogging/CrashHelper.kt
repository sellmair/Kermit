/*
 * Copyright (c) 2021 Touchlab
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package co.touchlab.kermit.crashlogging

import co.touchlab.kermit.Kermit
import platform.Foundation.NSUUID
import kotlin.native.concurrent.freeze

fun setupUnhandledExceptionHook(kermit: Kermit, onCrash: () -> String) {
    val unhandMe: ReportUnhandledExceptionHook = { t ->
        kermit.e(t, onCrash)
    }

    setUnhandledExceptionHook(unhandMe.freeze())
}

fun transformException(t: Throwable, block: (String, String, addresses: List<Long>) -> Unit) {
    fun throwableBoilerplate(frameString: String, lookFor: String) =
        !frameString.contains("kotlin.${lookFor}")
                &&
                !frameString.contains("${lookFor}.<init>")

    val addresses: List<Long> = t.getStackTraceAddresses()

    var index = 0

    val stackTrace = t.getStackTrace()
    for (element in stackTrace) {
        if (
            throwableBoilerplate(element, "Exception")
            ||
            throwableBoilerplate(element, "Throwable")
        ) {
            break
        }

        index++
    }

    val trimmedAddresses = addresses.subList(index, addresses.size)

    block(
        t::class.simpleName ?: "(Unknown Type)",
        t.message ?: "",
        trimmedAddresses
    )
}

fun generateCrashId():String = NSUUID().UUIDString.substring(0, 8)
val ktCrashKey = "ktcrash"