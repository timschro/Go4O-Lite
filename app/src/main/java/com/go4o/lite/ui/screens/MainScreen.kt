package com.go4o.lite.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.go4o.lite.R
import com.go4o.lite.data.model.AppSettings
import com.go4o.lite.data.model.Language
import com.go4o.lite.data.model.OverlayStyle
import com.go4o.lite.data.model.Course
import com.go4o.lite.data.model.ReadoutResult
import com.go4o.lite.data.model.ResultStatus
import com.go4o.lite.ui.strings.AppStrings
import com.go4o.lite.ui.strings.stringsFor
import com.go4o.lite.ui.viewmodel.MainUiState
import com.go4o.lite.ui.viewmodel.ReadoutEntry
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    uiState: MainUiState,
    onImportCourses: (android.net.Uri) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDismissResult: () -> Unit,
    onClearError: () -> Unit,
    onClearHistory: () -> Unit,
    onClearAll: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit = {},
    onPreviewResult: (ResultStatus) -> Unit = {},
    onDeleteCourse: (Int) -> Unit = {},
    onUpdateCourse: (Int, Course) -> Unit = { _, _ -> }
) {
    val strings = stringsFor(uiState.settings.language)
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(strings.tabHome) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Map, contentDescription = null) },
                        label = {
                            Text(
                                if (uiState.courses.isNotEmpty())
                                    "${strings.tabCourses} (${uiState.courses.size})"
                                else strings.tabCourses
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(painter = painterResource(R.drawable.ic_si_card), contentDescription = null) },
                        label = {
                            Text(
                                if (uiState.readoutHistory.isNotEmpty())
                                    "${strings.tabReadouts} (${uiState.readoutHistory.size})"
                                else strings.tabReadouts
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(strings.tabSettings) }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> StationTab(uiState, strings, onConnect, onDisconnect, onClearError, onClearAll)
                    1 -> CoursesTab(uiState, strings, onImportCourses, onDeleteCourse, onUpdateCourse)
                    2 -> ReadoutsTab(uiState, strings, onClearHistory)
                    3 -> SettingsTab(uiState, strings, onUpdateSettings, onPreviewResult)
                }
            }
        }

        // Result overlay on top of everything
        AnimatedVisibility(
            visible = uiState.currentResult != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.currentResult?.let { result ->
                ResultOverlay(
                    result = result,
                    strings = strings,
                    overlayStyle = uiState.settings.overlayStyle,
                    durationSeconds = uiState.settings.overlayDurationSeconds,
                    smileyPass = uiState.settings.smileyPass,
                    smileyFail = uiState.settings.smileyFail,
                    onDismiss = onDismissResult
                )
            }
        }
    }
}

// ── Station Tab ──

@Composable
private fun StationTab(
    uiState: MainUiState,
    strings: AppStrings,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onClearError: () -> Unit,
    onClearAll: () -> Unit
) {
    var showClearAllDialog by remember { mutableStateOf(false) }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(strings.clearAllDialogTitle) },
            text = { Text(strings.clearAllDialogText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllDialog = false
                        onClearAll()
                    }
                ) {
                    Text(strings.clearAllConfirm, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = strings.appTitle,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Connection card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = strings.station,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isConnected) Icons.Default.Usb else Icons.Default.UsbOff,
                        contentDescription = null,
                        tint = if (uiState.isConnected) Color(0xFF43A047) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.connectionStatus,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (uiState.isConnected) {
            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.UsbOff, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.disconnect)
            }
        } else {
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Usb, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.connectStation)
            }
        }

        // Error message
        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = onClearError) {
                        Text(strings.dismiss)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Clear All button
        if (uiState.courses.isNotEmpty() || uiState.readoutHistory.isNotEmpty()) {
            OutlinedButton(
                onClick = { showClearAllDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.clearAllData)
            }
        }
    }
}

// ── Courses Tab ──

@Composable
private fun CoursesTab(
    uiState: MainUiState,
    strings: AppStrings,
    onImportCourses: (android.net.Uri) -> Unit,
    onDeleteCourse: (Int) -> Unit,
    onUpdateCourse: (Int, Course) -> Unit
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportCourses(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.coursesHeading,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { filePickerLauncher.launch(arrayOf("text/xml", "application/xml", "*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.importCourses)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.courses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.coursesEmpty,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = strings.courseCount(uiState.courses.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(uiState.courses) { index, course ->
                    CourseCard(
                        course = course,
                        strings = strings,
                        onDeleteCourse = { onDeleteCourse(index) },
                        onUpdateCourse = { onUpdateCourse(index, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseCard(
    course: Course,
    strings: AppStrings,
    onDeleteCourse: () -> Unit,
    onUpdateCourse: (Course) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editControls by remember { mutableStateOf(course.controls.map { it.toString() }) }

    // Reset edit state when course changes externally
    LaunchedEffect(course) {
        editControls = course.controls.map { it.toString() }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteCourseDialogTitle) },
            text = { Text(strings.deleteCourseDialogText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteCourse()
                    }
                ) {
                    Text(strings.deleteCourse, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { if (!editing) expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = buildString {
                            append(strings.controlsInfo(course.controls.size))
                            course.length?.let { append(" · ${it}m") }
                            course.climb?.let { append(" · ↑${it}m") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) strings.collapse else strings.expand,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                if (editing) {
                    // Edit mode
                    editControls.forEachIndexed { i, value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${i + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp)
                            )
                            OutlinedTextField(
                                value = value,
                                onValueChange = { newValue ->
                                    editControls = editControls.toMutableList().also { it[i] = newValue }
                                },
                                label = { Text(strings.controlNumber) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                isError = value.toIntOrNull()?.let { it <= 0 } ?: true
                            )
                            if (editControls.size > 1) {
                                IconButton(onClick = {
                                    editControls = editControls.toMutableList().also { it.removeAt(i) }
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = strings.deleteControl,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            editControls = editControls + ""
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.addControl)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            editing = false
                            editControls = course.controls.map { it.toString() }
                        }) {
                            Text(strings.cancel)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val allValid = editControls.isNotEmpty() &&
                                editControls.all { it.toIntOrNull()?.let { n -> n > 0 } == true }
                        Button(
                            onClick = {
                                val newControls = editControls.mapNotNull { it.toIntOrNull() }
                                onUpdateCourse(course.copy(controls = newControls))
                                editing = false
                            },
                            enabled = allValid
                        ) {
                            Text(strings.save)
                        }
                    }
                } else {
                    // Read-only mode
                    course.controls.forEachIndexed { i, code ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${i + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp)
                            )
                            Text(
                                text = code.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = strings.deleteCourse,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = {
                            editControls = course.controls.map { it.toString() }
                            editing = true
                        }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = strings.editCourse
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Readouts Tab ──

@Composable
private fun ReadoutsTab(
    uiState: MainUiState,
    strings: AppStrings,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.readoutsHeading,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (uiState.readoutHistory.isNotEmpty()) {
                IconButton(onClick = onClearHistory) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = strings.clearAll,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.readoutHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.readoutsEmpty,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = strings.readoutCount(uiState.readoutHistory.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                reverseLayout = true
            ) {
                itemsIndexed(uiState.readoutHistory) { _, entry ->
                    ReadoutCard(entry = entry, strings = strings)
                }
            }
        }
    }
}

@Composable
private fun ReadoutCard(entry: ReadoutEntry, strings: AppStrings) {
    var expanded by remember { mutableStateOf(false) }
    val result = entry.result
    val statusColor = when (result.status) {
        ResultStatus.PASS -> Color(0xFF2E7D32)
        ResultStatus.FAIL -> Color(0xFFC62828)
        ResultStatus.NO_COURSE -> Color(0xFFE65100)
    }
    val statusLabel = when (result.status) {
        ResultStatus.PASS -> strings.statusPass
        ResultStatus.FAIL -> strings.statusMispunch
        ResultStatus.NO_COURSE -> strings.statusNoCourse
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Surface(
                    color = statusColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = statusLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.cardLabel(result.siCardResult.siNumber),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = buildString {
                            result.matchedCourse?.let { append(it.name) }
                            result.runningTimeMs?.let {
                                if (isNotEmpty()) append(" · ")
                                append(formatTime(it))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "#${entry.index}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(entry.timestampMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) strings.collapse else strings.expand,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Missing controls summary (always visible if any)
            if (result.missingControls.isNotEmpty() && !expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.missingLabel(result.missingControls.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }

            // Expanded details
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(strings.cardSeries, result.siCardResult.cardSeries)
                result.matchedCourse?.let {
                    DetailRow(strings.course, "${it.name} (${strings.controlsInfo(it.controls.size)})")
                }
                result.runningTimeMs?.let {
                    DetailRow(strings.time, formatTime(it))
                }
                DetailRow(strings.punches, "${result.siCardResult.punches.size}")

                if (result.missingControls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.missingControls,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = result.missingControls.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }

                // Punch list
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.punchLog,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                result.siCardResult.punches.forEachIndexed { i, punch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = "${i + 1}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = "${punch.code}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(48.dp)
                        )
                        if (punch.timestampMs >= 0) {
                            Text(
                                text = formatPunchTime(punch.timestampMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Result Overlay ──

@Composable
fun ResultOverlay(
    result: ReadoutResult,
    strings: AppStrings,
    overlayStyle: OverlayStyle = OverlayStyle.TEXT,
    durationSeconds: Int = 10,
    smileyPass: String = "\uD83D\uDE0A",
    smileyFail: String = "\uD83D\uDE1E",
    onDismiss: () -> Unit
) {
    LaunchedEffect(result) {
        delay(durationSeconds * 1000L)
        onDismiss()
    }

    val backgroundColor = when (result.status) {
        ResultStatus.PASS -> Color(0xFF2E7D32)
        ResultStatus.FAIL -> Color(0xFFC62828)
        ResultStatus.NO_COURSE -> Color(0xFFE65100)
    }

    val statusLabel = when (result.status) {
        ResultStatus.PASS -> strings.statusPass
        ResultStatus.FAIL -> strings.statusMispunch
        ResultStatus.NO_COURSE -> strings.statusNoCourse
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            when (overlayStyle) {
                OverlayStyle.TEXT -> {
                    val icon = when (result.status) {
                        ResultStatus.PASS -> Icons.Default.CheckCircle
                        ResultStatus.FAIL -> Icons.Default.Cancel
                        ResultStatus.NO_COURSE -> Icons.Default.Help
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(120.dp)
                    )

                    result.runningTimeMs?.let { timeMs ->
                        Text(
                            text = formatTime(timeMs),
                            color = Color.White,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = statusLabel,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = strings.cardLabel(result.siCardResult.siNumber),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 24.sp
                    )

                    result.matchedCourse?.let { course ->
                        Text(
                            text = course.name,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 20.sp
                        )
                    }

                    if (result.missingControls.isNotEmpty()) {
                        Text(
                            text = strings.missingLabel(result.missingControls.joinToString(", ")),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                OverlayStyle.SMILEY -> {
                    val emoji = when (result.status) {
                        ResultStatus.PASS -> smileyPass
                        ResultStatus.FAIL -> smileyFail
                        ResultStatus.NO_COURSE -> "\u2753"
                    }
                    Text(
                        text = emoji,
                        fontSize = 200.sp,
                        textAlign = TextAlign.Center
                    )

                    result.runningTimeMs?.let { timeMs ->
                        Text(
                            text = formatTime(timeMs),
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = strings.cardLabel(result.siCardResult.siNumber),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 24.sp
                    )

                    if (result.missingControls.isNotEmpty()) {
                        Text(
                            text = strings.missingLabel(result.missingControls.joinToString(", ")),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = strings.tapToDismiss,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}

// ── Settings Tab ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTab(
    uiState: MainUiState,
    strings: AppStrings,
    onUpdateSettings: (AppSettings) -> Unit,
    onPreviewResult: (ResultStatus) -> Unit
) {
    val settings = uiState.settings
    var durationSliderValue by remember(settings.overlayDurationSeconds) {
        mutableStateOf(settings.overlayDurationSeconds.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.settingsHeading,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Language
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = strings.language,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = settings.language.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Language.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.displayName) },
                                onClick = {
                                    expanded = false
                                    onUpdateSettings(settings.copy(language = lang))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Keep Screen On
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.keepScreenOn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = settings.keepScreenOn,
                    onCheckedChange = { onUpdateSettings(settings.copy(keepScreenOn = it)) }
                )
            }
        }

        // Overlay Duration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = strings.overlayDuration,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.seconds(durationSliderValue.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = durationSliderValue,
                    onValueChange = { durationSliderValue = it },
                    onValueChangeFinished = {
                        onUpdateSettings(settings.copy(overlayDurationSeconds = durationSliderValue.toInt()))
                    },
                    valueRange = 3f..30f,
                    steps = 26
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("3s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("30s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Overlay Style
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = strings.overlayStyle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OverlayStyle.entries.forEach { style ->
                        val label = when (style) {
                            OverlayStyle.TEXT -> strings.styleText
                            OverlayStyle.SMILEY -> strings.styleSmiley
                        }
                        FilterChip(
                            selected = settings.overlayStyle == style,
                            onClick = { onUpdateSettings(settings.copy(overlayStyle = style)) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Smiley Emoji Customization (only when SMILEY style selected)
        if (settings.overlayStyle == OverlayStyle.SMILEY) {
            var showPassPicker by remember { mutableStateOf(false) }
            var showFailPicker by remember { mutableStateOf(false) }

            if (showPassPicker) {
                EmojiPickerDialog(
                    title = strings.smileyPassEmoji,
                    selected = settings.smileyPass,
                    onSelect = {
                        onUpdateSettings(settings.copy(smileyPass = it))
                        showPassPicker = false
                    },
                    onDismiss = { showPassPicker = false }
                )
            }
            if (showFailPicker) {
                EmojiPickerDialog(
                    title = strings.smileyFailEmoji,
                    selected = settings.smileyFail,
                    onSelect = {
                        onUpdateSettings(settings.copy(smileyFail = it))
                        showFailPicker = false
                    },
                    onDismiss = { showFailPicker = false }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.styleSmiley,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        EmojiPickerButton(
                            label = strings.smileyPassEmoji,
                            emoji = settings.smileyPass,
                            onClick = { showPassPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        EmojiPickerButton(
                            label = strings.smileyFailEmoji,
                            emoji = settings.smileyFail,
                            onClick = { showFailPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Preview Buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = strings.previewOverlay,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onPreviewResult(ResultStatus.PASS) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text(strings.previewSuccess)
                    }
                    Button(
                        onClick = { onPreviewResult(ResultStatus.FAIL) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) {
                        Text(strings.previewFail)
                    }
                }
            }
        }
    }
}

// ── Emoji Picker ──

private val emojiList = listOf(
    "\uD83D\uDE0A", "\uD83D\uDE00", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE01", "\uD83D\uDE06",
    "\uD83D\uDE0D", "\uD83E\uDD29", "\uD83D\uDE0E", "\uD83E\uDD73", "\uD83E\uDD70", "\uD83D\uDE07",
    "\uD83D\uDE0B", "\uD83E\uDD11", "\uD83E\uDD2A", "\uD83E\uDD17", "\uD83E\uDD2D", "\uD83D\uDE09",
    "\uD83D\uDE1E", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE23", "\uD83D\uDE29", "\uD83D\uDE21",
    "\uD83D\uDE20", "\uD83E\uDD2C", "\uD83D\uDE31", "\uD83D\uDE28", "\uD83D\uDE30", "\uD83E\uDD25",
    "\uD83D\uDE15", "\uD83D\uDE41", "\uD83D\uDE44", "\uD83E\uDD14", "\uD83E\uDD28", "\uD83D\uDE12",
    "\u2705", "\u274C", "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83C\uDF1F", "\u2B50",
    "\uD83C\uDFC6", "\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49", "\uD83D\uDCA5", "\uD83D\uDCA9",
    "\u2764\uFE0F", "\uD83D\uDC94", "\uD83D\uDD25", "\u26A0\uFE0F", "\uD83D\uDEA8", "\uD83C\uDF89"
)

@Composable
private fun EmojiPickerButton(
    label: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 40.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmojiPickerDialog(
    title: String,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            val columns = 6
            Column {
                emojiList.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { emoji ->
                            val isSelected = emoji == selected
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable { onSelect(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 28.sp)
                            }
                        }
                        // Fill remaining cells in the last row
                        repeat(columns - row.size) {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

// ── Formatters ──

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatPunchTime(timestampMs: Long): String {
    val totalSeconds = timestampMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d:%02d", hours, minutes, seconds)
}

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

private fun formatTimestamp(timestampMs: Long): String {
    return timeFormat.format(Date(timestampMs))
}
