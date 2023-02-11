/*
Copyright (C) 2011-2014 Sublime Software Ltd

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/
package net.veldor.tor_client.model.control

import android.os.FileObserver
import net.veldor.tor_client.model.tor_utils.WriteObserver
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Adapted from the Briar WriteObserver code
 */
class AndroidWriteObserver(file: File) : FileObserver(file.absolutePath, CLOSE_WRITE),
    WriteObserver {
    private val countDownLatch = CountDownLatch(1)

    init {
        require(file.exists()) { "FileObserver doesn't work properly on files that don't already exist." }
        startWatching()
    }

    override fun poll(timeout: Long, unit: TimeUnit?): Boolean {
        return try {
            countDownLatch.await(timeout, unit)
        } catch (e: InterruptedException) {
            throw RuntimeException(
                "Internal error has caused AndroidWriteObserver to not be reliable.",
                e
            )
        }
    }

    override fun onEvent(i: Int, s: String?) {
        stopWatching()
        countDownLatch.countDown()
    }
}