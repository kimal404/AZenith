/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.subscreens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveListItem
import zx.azenith.ui.component.ExpressiveSliderItem
import zx.azenith.ui.navigation.safePopBackStack
import zx.azenith.ui.util.ZramUtil

@Composable
fun ZramSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isApplying by remember { mutableStateOf(false) }
    var showAlgoDialog by remember { mutableStateOf(false) }
    var zramSize by remember { mutableFloatStateOf(4f) }
    var swappiness by remember { mutableFloatStateOf(60f) }
    var algorithms by remember { mutableStateOf(listOf("lz4", "zstd", "lzo", "lzo-rle")) }
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

    if (showAlgoDialog) {
        AlertDialog(
            onDismissRequest = { showAlgoDialog = false },
            title = { Text(text = "Algoritma Kompresi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    algorithms.forEach { algo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAlgo = algo
                                    showAlgoDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (algo == selectedAlgo),
                                onClick = {
                                    selectedAlgo = algo
                                    showAlgoDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = algo,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlgoDialog = false }) {
                    Text(text = "Tutup")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "ZRAM & Swap Manager") },
                navigationIcon = {
                    IconButton(onClick = { navController.safePopBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
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
                                ExpressiveListItem(
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        if (!isApplying && zramSize > 0f) {
                                            showAlgoDialog = true
                                        }
                                    },
                                    headlineContent = { Text(text = "Algoritma Kompresi") },
                                    supportingContent = { Text(text = "Pilihan aktif: $selectedAlgo") },
                                    trailingContent = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null
                                        )
                                    }
                                )
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
                            }
                        )
                    )
                }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
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
            }
        }
    }
}
