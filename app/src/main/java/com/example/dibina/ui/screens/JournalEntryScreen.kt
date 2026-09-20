package com.example.dibina.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dibina.domain.DateValidationResult
import com.example.dibina.domain.ExpCalculator
import com.example.dibina.domain.JournalConstants
import com.example.dibina.domain.JournalIdHelper
import com.example.dibina.model.JournalEntry
import com.example.dibina.model.StudentProfile
import com.example.ui.theme.DibinaGoldExp
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

// High contrast colors for high readability
private val DarkTextColor = Color(0xFF0F172A)
private val MutedTextColor = Color(0xFF64748B)
private val CardBorderColor = Color(0xFFE2E8F0)
private val SuccessGreen = Color(0xFF16A34A)
private val LightSuccessGreen = Color(0xFFF0FDF4)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JournalEntryScreen(
    studentProfile: StudentProfile,
    initialJournal: JournalEntry?,
    onFetchJournalForDate: suspend (String) -> JournalEntry?,
    onSaveJournal: suspend (JournalEntry) -> Result<Unit>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Date state: default to today or initialJournal date
    val todayDateString = remember { JournalIdHelper.getTodayDateString() }
    val selectablePastDates = remember { JournalIdHelper.getSelectablePastDates() }
    var selectedDateString by remember { mutableStateOf(initialJournal?.date ?: todayDateString) }
    var currentJournal by remember { mutableStateOf(initialJournal) }
    var isCheckingDate by remember { mutableStateOf(false) }

    val isEditMode = currentJournal != null

    // 1. Bangun Pagi (Time Picker)
    var bangunPagiTime by remember { mutableStateOf(initialJournal?.bangunPagiTime ?: "") }

    // 2. Beribadah (Religion specific)
    val isMuslim = remember(studentProfile.religion) {
        studentProfile.religion.contains("Islam", ignoreCase = true) || studentProfile.religion.isBlank()
    }
    val selectedPrayers = remember { mutableStateListOf<String>() }
    var nonMuslimDevotionText by remember { mutableStateOf(initialJournal?.ibadahDetails ?: "") }

    // 3. Berolahraga (Short text + recommendations)
    var olahragaActivity by remember { mutableStateOf(initialJournal?.olahragaActivity ?: "") }

    // 4. Makan Sehat dan Bergizi (Strictly NO mineral field!)
    var karbohidratText by remember { mutableStateOf(initialJournal?.karbohidratText ?: "") }
    var proteinText by remember { mutableStateOf(initialJournal?.proteinText ?: "") }
    var lemakText by remember { mutableStateOf(initialJournal?.lemakText ?: "") }
    var vitaminText by remember { mutableStateOf(initialJournal?.vitaminText ?: "") }
    var seratText by remember { mutableStateOf(initialJournal?.seratText ?: "") }
    var airGelas by remember { mutableIntStateOf(initialJournal?.airGelas ?: 8) }

    // 5. Gemar Belajar (Materi + Duration with 15-min increments)
    var materiBelajar by remember { mutableStateOf(initialJournal?.materiBelajar ?: "") }
    var durasiBelajar by remember { mutableIntStateOf(if (initialJournal?.durasiBelajar != null && initialJournal.durasiBelajar > 0) initialJournal.durasiBelajar else 30) }

    // 6. Bermasyarakat (Short text + recommended choices)
    var bermasyarakatActivity by remember { mutableStateOf(initialJournal?.bermasyarakatActivity ?: "") }

    // 7. Tidur Cepat (Time Picker)
    var tidurCepatTime by remember { mutableStateOf(initialJournal?.tidurCepatTime ?: "") }

    var isSaving by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successDialogMessage by remember { mutableStateOf("") }

    // Initialize prayer checklist if editing
    LaunchedEffect(currentJournal) {
        currentJournal?.let { j ->
            bangunPagiTime = j.bangunPagiTime
            selectedPrayers.clear()
            selectedPrayers.addAll(j.ibadahSholat)
            nonMuslimDevotionText = j.ibadahDetails
            olahragaActivity = j.olahragaActivity
            karbohidratText = j.karbohidratText
            proteinText = j.proteinText
            lemakText = j.lemakText
            vitaminText = j.vitaminText
            seratText = j.seratText
            airGelas = if (j.airGelas > 0) j.airGelas else 8
            materiBelajar = j.materiBelajar
            durasiBelajar = if (j.durasiBelajar > 0) j.durasiBelajar else 30
            bermasyarakatActivity = j.bermasyarakatActivity
            tidurCepatTime = j.tidurCepatTime
        }
    }

    // Function to reload journal when date is changed
    fun loadJournalForDate(dateStr: String) {
        selectedDateString = dateStr
        isCheckingDate = true
        coroutineScope.launch {
            val fetched = onFetchJournalForDate(dateStr)
            currentJournal = fetched
            if (fetched == null) {
                // Reset to fresh form
                bangunPagiTime = ""
                selectedPrayers.clear()
                nonMuslimDevotionText = ""
                olahragaActivity = ""
                karbohidratText = ""
                proteinText = ""
                lemakText = ""
                vitaminText = ""
                seratText = ""
                airGelas = 8
                materiBelajar = ""
                durasiBelajar = 30
                bermasyarakatActivity = ""
                tidurCepatTime = ""
            }
            isCheckingDate = false
        }
    }

    // Habit completion calculations
    val habit1Done = bangunPagiTime.isNotBlank()
    val habit2Done = if (isMuslim) selectedPrayers.isNotEmpty() else nonMuslimDevotionText.isNotBlank()
    val habit3Done = olahragaActivity.isNotBlank()

    val nutritionElementsCount = listOf(
        karbohidratText.isNotBlank(),
        proteinText.isNotBlank(),
        lemakText.isNotBlank(),
        vitaminText.isNotBlank(),
        seratText.isNotBlank(),
        airGelas >= 1
    ).count { it }
    val habit4Done = nutritionElementsCount >= 3

    val habit5Done = materiBelajar.isNotBlank() && durasiBelajar >= 15
    val habit6Done = bermasyarakatActivity.isNotBlank()
    val habit7Done = tidurCepatTime.isNotBlank()

    val completedCount = listOf(
        habit1Done, habit2Done, habit3Done,
        habit4Done, habit5Done, habit6Done, habit7Done
    ).count { it }

    val potentialExp = ExpCalculator.calculateExp(completedCount)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEditMode) "Edit Jurnal 7 Kebiasaan" else "Catat 7 Kebiasaan Baik",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor
                        )
                        Text(
                            text = if (isEditMode) "Memperbarui catatan harian" else "Anak Indonesia Hebat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedTextColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = DarkTextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // DATE SELECTOR SECTION (Allowed: Today + up to 7 days in past)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("date_selector_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorderColor))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tanggal Jurnal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextColor
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Mode Edit",
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pilih tanggal (Hari ini atau maksimal 7 hari ke belakang):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedTextColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontal selectable date chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectablePastDates) { selectableDate ->
                                val isSelected = selectableDate.dateString == selectedDateString
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (selectedDateString != selectableDate.dateString) {
                                            loadJournalForDate(selectableDate.dateString)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = selectableDate.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else DarkTextColor
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = Color(0xFFF1F5F9)
                                    )
                                )
                            }
                        }

                        if (isCheckingDate) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Memeriksa catatan tanggal...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedTextColor
                                )
                            }
                        }
                    }
                }
            }

            // EXP & PROGRESS HEADER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("journal_exp_header_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pencapaian 7 Kebiasaan Hebat",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$completedCount dari 7 Kebiasaan Terpenuhi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DibinaGoldExp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+$potentialExp EXP",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 1. BANGUN PAGI (Time Picker)
            // ==========================================
            item {
                HabitContainerCard(
                    number = 1,
                    title = "Bangun Pagi",
                    subtitle = "Bangun pagi dengan ceria dan mandiri (rekomendasi: 04.00 - 05.00)",
                    icon = Icons.Default.WbSunny,
                    isCompleted = habit1Done
                ) {
                    Text(
                        text = "Jam berapa kamu bangun?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkTextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset buttons + TimePickerDialog trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("04.00", "04.30", "05.00").forEach { preset ->
                            val isSelected = bangunPagiTime == preset
                            OutlinedButton(
                                onClick = { bangunPagiTime = preset },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) MaterialTheme.colorScheme.primary else CardBorderColor)
                                )
                            ) {
                                Text(
                                    text = preset,
                                    color = if (isSelected) Color.White else DarkTextColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        // Custom Time Picker Button
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        bangunPagiTime = String.format(Locale.US, "%02d.%02d", hourOfDay, minute)
                                    },
                                    4, 0, true
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (bangunPagiTime.isNotBlank()) bangunPagiTime else "Pilih Jam",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. BERIBADAH (Religion specific)
            // ==========================================
            item {
                HabitContainerCard(
                    number = 2,
                    title = "Beribadah",
                    subtitle = if (isMuslim) "Ibadah sholat 5 waktu tepat waktu" else "Menjalankan doa dan ibadah sesuai ajaran agama",
                    icon = Icons.Default.SelfImprovement,
                    isCompleted = habit2Done
                ) {
                    if (isMuslim) {
                        Text(
                            text = "Sholat yang telah kamu tunaikan hari ini:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            JournalConstants.MUSLIM_PRAYERS.forEach { prayer ->
                                val isChecked = selectedPrayers.contains(prayer)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChecked) LightSuccessGreen else Color(0xFFF1F5F9))
                                        .clickable {
                                            if (isChecked) selectedPrayers.remove(prayer)
                                            else selectedPrayers.add(prayer)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedPrayers.add(prayer)
                                            else selectedPrayers.remove(prayer)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = SuccessGreen)
                                    )
                                    Text(
                                        text = prayer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                        color = DarkTextColor
                                    )
                                }
                            }
                        }
                    } else {
                        // Appropriate non-Muslim devotion input
                        Text(
                            text = "Aktivitas ibadah / doa yang kamu lakukan:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            JournalConstants.NON_MUSLIM_DEVOTIONS.forEach { devotion ->
                                val isSelected = nonMuslimDevotionText.contains(devotion)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        nonMuslimDevotionText = if (isSelected) {
                                            nonMuslimDevotionText.replace(devotion, "").trim().trim(',')
                                        } else {
                                            if (nonMuslimDevotionText.isBlank()) devotion else "$nonMuslimDevotionText, $devotion"
                                        }
                                    },
                                    label = { Text(devotion, color = DarkTextColor) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HighContrastTextField(
                            value = nonMuslimDevotionText,
                            onValueChange = { nonMuslimDevotionText = it },
                            placeholder = "Tuliskan kegiatan doa / ibadahmu hari ini...",
                            label = "Catatan Ibadah"
                        )
                    }
                }
            }

            // ==========================================
            // 3. BEROLAHRAGA (Short text + chips)
            // ==========================================
            item {
                HabitContainerCard(
                    number = 3,
                    title = "Berolahraga",
                    subtitle = "Olahraga apa yang kamu lakukan? (min. 15–30 menit)",
                    icon = Icons.Default.DirectionsRun,
                    isCompleted = habit3Done
                ) {
                    HighContrastTextField(
                        value = olahragaActivity,
                        onValueChange = { olahragaActivity = it },
                        label = "Berolahraga",
                        placeholder = "Olahraga apa yang kamu lakukan?"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rekomendasi cepat:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        JournalConstants.EXERCISE_SUGGESTIONS.forEach { ex ->
                            SuggestionChip(
                                onClick = { olahragaActivity = ex },
                                label = { Text(ex, color = DarkTextColor, fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 4. MAKAN SEHAT DAN BERGIZI (NO MINERAL FIELD!)
            // ==========================================
            item {
                HabitContainerCard(
                    number = 4,
                    title = "Makan Sehat dan Bergizi",
                    subtitle = "Isi unsur gizi yang kamu konsumsi (terpenuhi jika minimal 3 unsur)",
                    icon = Icons.Default.Restaurant,
                    isCompleted = habit4Done
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Karbohidrat
                        NutrientInputField(
                            name = "Karbohidrat",
                            value = karbohidratText,
                            onValueChange = { karbohidratText = it },
                            placeholder = "Contoh: Nasi, Mie, Ubi, dll."
                        )

                        // Protein
                        NutrientInputField(
                            name = "Protein",
                            value = proteinText,
                            onValueChange = { proteinText = it },
                            placeholder = "Contoh: Telur, Ikan, Ayam, Tempe, dll."
                        )

                        // Lemak
                        NutrientInputField(
                            name = "Lemak",
                            value = lemakText,
                            onValueChange = { lemakText = it },
                            placeholder = "Contoh: Ikan, Kacang, Alpukat, dll."
                        )

                        // Vitamin
                        NutrientInputField(
                            name = "Vitamin",
                            value = vitaminText,
                            onValueChange = { vitaminText = it },
                            placeholder = "Contoh: Pisang, Apel, Jeruk, dll."
                        )

                        // Serat
                        NutrientInputField(
                            name = "Serat",
                            value = seratText,
                            onValueChange = { seratText = it },
                            placeholder = "Contoh: Wortel, Bayam, Buncis, dll."
                        )

                        // Air (Selector: 1 gelas, 2 gelas, ..., 12 gelas)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalDrink,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Air Putih: $airGelas gelas hari ini",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(JournalConstants.WATER_GLASS_OPTIONS) { count ->
                                    val isSelected = airGelas == count
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { airGelas = count },
                                        label = { Text("$count gelas", color = if (isSelected) Color.White else DarkTextColor) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF0284C7),
                                            containerColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 5. GEMAR BELAJAR (Materi + 15-min selector)
            // ==========================================
            item {
                HabitContainerCard(
                    number = 5,
                    title = "Gemar Belajar",
                    subtitle = "Mempelajari pengetahuan baru dan membaca buku",
                    icon = Icons.Default.MenuBook,
                    isCompleted = habit5Done
                ) {
                    HighContrastTextField(
                        value = materiBelajar,
                        onValueChange = { materiBelajar = it },
                        label = "Materi",
                        placeholder = "Contoh: Pecahan, Ide Pokok, Pencernaan, dll."
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Waktu Belajar: $durasiBelajar menit (kelipatan 15 menit):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkTextColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(JournalConstants.STUDY_DURATION_OPTIONS) { minutes ->
                            val isSelected = durasiBelajar == minutes
                            FilterChip(
                                selected = isSelected,
                                onClick = { durasiBelajar = minutes },
                                label = {
                                    Text(
                                        text = "$minutes menit",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else DarkTextColor
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    containerColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 6. BERMASYARAKAT (Short text + choices)
            // ==========================================
            item {
                HabitContainerCard(
                    number = 6,
                    title = "Bermasyarakat",
                    subtitle = "Berbuat baik dan menolong sesama orang di sekitarmu",
                    icon = Icons.Default.VolunteerActivism,
                    isCompleted = habit6Done
                ) {
                    HighContrastTextField(
                        value = bermasyarakatActivity,
                        onValueChange = { bermasyarakatActivity = it },
                        label = "Bermasyarakat",
                        placeholder = "Apa kebaikan yang kamu lakukan hari ini?"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pilihan contoh kebaikan:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        JournalConstants.COMMUNITY_SUGGESTIONS.forEach { act ->
                            SuggestionChip(
                                onClick = { bermasyarakatActivity = act },
                                label = { Text(act, color = DarkTextColor, fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 7. TIDUR CEPAT (Time Picker)
            // ==========================================
            item {
                HabitContainerCard(
                    number = 7,
                    title = "Tidur Cepat",
                    subtitle = "Istirahat cukup tepat waktu (rekomendasi: 19.30 - 21.00)",
                    icon = Icons.Default.Bedtime,
                    isCompleted = habit7Done
                ) {
                    Text(
                        text = "Jam berapa kamu tidur?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkTextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("19.30", "20.00", "20.30", "21.00").forEach { preset ->
                            val isSelected = tidurCepatTime == preset
                            OutlinedButton(
                                onClick = { tidurCepatTime = preset },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) MaterialTheme.colorScheme.primary else CardBorderColor)
                                )
                            ) {
                                Text(
                                    text = preset,
                                    color = if (isSelected) Color.White else DarkTextColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        Button(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        tidurCepatTime = String.format(Locale.US, "%02d.%02d", hourOfDay, minute)
                                    },
                                    19, 30, true
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (tidurCepatTime.isNotBlank()) tidurCepatTime else "Pilih Jam",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SUBMIT BUTTON (Save / Update Flow)
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // Validate date range
                        val dateCheck = JournalIdHelper.validateJournalDate(selectedDateString)
                        if (dateCheck is DateValidationResult.Invalid) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(dateCheck.reason)
                            }
                            return@Button
                        }

                        // Validate 15-min multiples for study duration
                        if (durasiBelajar % 15 != 0) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Waktu belajar harus kelipatan 15 menit.")
                            }
                            return@Button
                        }

                        isSaving = true
                        coroutineScope.launch {
                            val deterministicJournalId = JournalIdHelper.generateJournalId(
                                uid = studentProfile.uid,
                                dateString = selectedDateString
                            )

                            val entry = JournalEntry(
                                journalId = deterministicJournalId,
                                uid = studentProfile.uid,
                                classId = studentProfile.classId.orEmpty(),
                                date = selectedDateString,
                                // 1. Bangun Pagi
                                bangunPagi = habit1Done,
                                bangunPagiTime = bangunPagiTime,
                                // 2. Beribadah
                                ibadah = habit2Done,
                                ibadahSholat = selectedPrayers.toList(),
                                ibadahDetails = nonMuslimDevotionText,
                                // 3. Berolahraga
                                olahraga = habit3Done,
                                olahragaActivity = olahragaActivity,
                                // 4. Makan Sehat dan Bergizi (Strictly NO mineral field!)
                                karbohidrat = karbohidratText.isNotBlank(),
                                karbohidratText = karbohidratText,
                                protein = proteinText.isNotBlank(),
                                proteinText = proteinText,
                                lemak = lemakText.isNotBlank(),
                                lemakText = lemakText,
                                vitamin = vitaminText.isNotBlank(),
                                vitaminText = vitaminText,
                                serat = seratText.isNotBlank(),
                                seratText = seratText,
                                air = airGelas >= 1,
                                airGelas = airGelas,
                                // 5. Gemar Belajar
                                materiBelajar = materiBelajar,
                                durasiBelajar = durasiBelajar,
                                // 6. Bermasyarakat
                                bermasyarakat = habit6Done,
                                bermasyarakatActivity = bermasyarakatActivity,
                                // 7. Tidur Cepat
                                tidurCepat = habit7Done,
                                tidurCepatTime = tidurCepatTime,
                                exp = potentialExp,
                                isBackdate = (dateCheck as DateValidationResult.Valid).isBackdate,
                                createdAt = currentJournal?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )

                            val result = onSaveJournal(entry)
                            isSaving = false
                            result.onSuccess {
                                successDialogMessage = if (isEditMode) {
                                    "Jurnal tanggal ${JournalIdHelper.formatShortDate(selectedDateString)} berhasil diperbarui!"
                                } else {
                                    "Hebat! Jurnal berhasil disimpan. Kamu mendapatkan +$potentialExp EXP!"
                                }
                                showSuccessDialog = true
                            }.onFailure { err ->
                                snackbarHostState.showSnackbar(err.localizedMessage ?: "Gagal menyimpan jurnal.")
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_journal_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditMode) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditMode) "Perbarui Jurnal (${JournalIdHelper.formatShortDate(selectedDateString)})"
                            else "Simpan Jurnal (${JournalIdHelper.formatShortDate(selectedDateString)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // SUCCESS DIALOG
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateBack()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditMode) "Jurnal Diperbarui" else "Alhamdulillah, Berhasil!",
                        fontWeight = FontWeight.Bold,
                        color = DarkTextColor
                    )
                }
            },
            text = {
                Text(
                    text = successDialogMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkTextColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Kembali ke Beranda", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ==========================================
// REUSABLE COMPONENTS WITH HIGH TEXT CONTRAST
// ==========================================

@Composable
private fun HabitContainerCard(
    number: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) LightSuccessGreen else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isCompleted) Color(0xFFBBF7D0) else CardBorderColor)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$number. $title",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor
                        )
                        if (isCompleted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun NutrientInputField(
    name: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    HighContrastTextField(
        value = value,
        onValueChange = onValueChange,
        label = name,
        placeholder = placeholder,
        modifier = modifier
    )
}

@Composable
private fun HighContrastTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = DarkTextColor) },
        placeholder = { Text(placeholder, color = MutedTextColor) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        textStyle = TextStyle(
            color = DarkTextColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = DarkTextColor,
            unfocusedTextColor = DarkTextColor,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = CardBorderColor,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = DarkTextColor
        )
    )
}
