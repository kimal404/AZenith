/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.azenith.ui.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ZramUtil {
    private const val ZRAM_BLOCK = "/dev/block/zram0"
    private const val ZRAM_DISKSIZE = "/sys/block/zram0/disksize"
    private const val ZRAM_ALGO = "/sys/block/zram0/comp_algorithm"
    private const val ZRAM_RESET = "/sys/block/zram0/reset"
    private const val SWAPPINESS_NODE = "/proc/sys/vm/swappiness"

    suspend fun getZramSizeGb(): Float = withContext(Dispatchers.IO) {
        val result = Shell.cmd("cat $ZRAM_DISKSIZE").exec()
        if (result.isSuccess && result.out.isNotEmpty()) {
            val bytes = result.out[0].trim().toLongOrNull() ?: 0L
            val gb = (bytes / (1024L * 1024L * 1024L)).toFloat()
            return@withContext gb
        }
        4f
    }

    suspend fun getAlgorithms(): Pair<List<String>, String> = withContext(Dispatchers.IO) {
        val result = Shell.cmd("cat $ZRAM_ALGO").exec()
        val defaultList = listOf("lz4", "zstd", "lzo")
        var current = "lz4"
        if (result.isSuccess && result.out.isNotEmpty()) {
            val line = result.out[0].trim()
            val tokens = line.split(" ").filter { it.isNotBlank() }
            val list = mutableListOf<String>()
            for (token in tokens) {
                if (token.startsWith("[") && token.endsWith("]")) {
                    val raw = token.substring(1, token.length - 1)
                    current = raw
                    list.add(raw)
                } else {
                    list.add(token)
                }
            }
            if (list.isNotEmpty()) {
                return@withContext Pair(list, current)
            }
        }
        Pair(defaultList, current)
    }

    suspend fun getSwappiness(): Float = withContext(Dispatchers.IO) {
        val result = Shell.cmd("cat $SWAPPINESS_NODE").exec()
        if (result.isSuccess && result.out.isNotEmpty()) {
            val value = result.out[0].trim().toFloatOrNull() ?: 60f
            return@withContext value
        }
        60f
    }

    suspend fun applyZram(sizeGb: Int, algo: String, swappiness: Int): Boolean = withContext(Dispatchers.IO) {
        val bytes = sizeGb.toLong() * 1024L * 1024L * 1024L
        val commands = mutableListOf<String>()
        commands.add("swapoff $ZRAM_BLOCK")
        commands.add("echo 1 > $ZRAM_RESET")
        if (sizeGb > 0) {
            commands.add("echo $algo > $ZRAM_ALGO")
            commands.add("echo $bytes > $ZRAM_DISKSIZE")
            commands.add("mkswap $ZRAM_BLOCK")
            commands.add("swapon $ZRAM_BLOCK")
        }
        commands.add("echo $swappiness > $SWAPPINESS_NODE")
        val result = Shell.cmd(*commands.toTypedArray()).exec()
        result.isSuccess
    }
}
