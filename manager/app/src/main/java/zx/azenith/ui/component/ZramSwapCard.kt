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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.component

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import zx.azenith.ui.util.ZramUtil

@Composable
fun ZramSwapCard() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isApplying by remember { mutableStateOf(false) }
    var zramSize by remember { mutableStateOf(4f) }
    var swappiness by remember { mutableStateOf(60f) }
    var algorithms by remember { mutableStateOf(listOf("lz4", "zstd", "lzo")) }
    var selectedAlgo by remember { mutableStateOf("lz4") }

    LaunchedEffect(Unit) {
        val currentSize = ZramUtil.getZramSizeGb()
        val (algoList, currentAlgo) = ZramUtil.getAlgorithms()
        val currentSwappiness = ZramUtil.getSwappiness()
        zramSize = currentSize
        algorithms = algoList
        selectedAlgo = currentAlgo
        swappiness = currentSwappiness
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    } else {
        ExpressiveList(
            content = listOf(
                {
                    ExpressiveSliderItem(
                        icon = Icons.Rounded.Speed,
                        title = "Custom Swap Size",
                        subtitle = "Kapasitas alokasi ZRAM / Swap disk",
                        badgeText = if (zramSize == 0f) "0 GB (Off)" else "${zramSize.toInt()} GB",
                        sliderPosition = zramSize,
                        valueRange = 0f..16f,
                        steps = 15,
                        enabled = !isApplying,
                        onValueChange = { zramSize = it },
                        onValueChangeFinished = {}
                    )
                },
                {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Compression Algorithm",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Algoritma kompresi data memori kernel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            algorithms.forEach { algo ->
                                FilterChip(
                                    selected = algo == selectedAlgo,
                                    onClick = { selectedAlgo = algo },
                                    label = { Text(text = algo) },
                                    enabled = !isApplying && zramSize > 0f
                                )
                            }
                        }
                    }
                },
                {
                    ExpressiveSliderItem(
                        icon = Icons.Rounded.Speed,
                        title = "Kernel Swappiness",
                        subtitle = "Agresivitas kernel memindahkan memori aktif ke swap",
                        badgeText = "${swappiness.toInt()}",
                        sliderPosition = swappiness,
                        valueRange = 0f..200f,
                        steps = 199,
                        enabled = !isApplying,
                        onValueChange = { swappiness = it },
                        onValueChangeFinished = {}
                    )
                },
                {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isApplying = true
                                    val success = ZramUtil.applyZram(
                                        sizeGb = zramSize.toInt(),
                                        algo = selectedAlgo,
                                        swappiness = swappiness.toInt()
                                    )
                                    isApplying = false
                                    val msg = if (success) "ZRAM & Swap berhasil diterapkan!" else "Gagal menerapkan ZRAM"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isApplying
                        ) {
                            if (isApplying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(text = if (isApplying) "Menerapkan..." else "Terapkan Perubahan")
                        }
                    }
                }
            )
        )
    }
}
