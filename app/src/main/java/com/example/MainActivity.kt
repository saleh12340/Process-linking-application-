package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Models ---
enum class ServerStatus(val labelArabic: String, val color: Color) {
  CONNECTED("متصل", Emerald500),
  RECONNECTING("جاري الاتصال", Amber500),
  OFFLINE("غير متصل", Rose500)
}

enum class TransactionType(val labelArabic: String, val isCredit: Boolean) {
  CREDIT("دائن / إيداع", true),
  DEBIT("مدين / صرف", false);

  companion object {
    fun fromString(typeStr: String): TransactionType {
      val normalized = typeStr.trim().lowercase()
      return if (normalized.contains("credit") || normalized.contains("إيداع") ||
        normalized.contains("له") || normalized.contains("قبض") ||
        normalized.contains("دخل") || normalized.contains("in") || normalized == "cr"
      ) {
        CREDIT
      } else {
        DEBIT
      }
    }
  }
}

data class RecordItem(
  val id: String,
  val title: String,
  val amount: Double,
  val timestamp: Long,
  val details: String,
  val type: TransactionType,
  val rawDateString: String = ""
)

data class DashboardUiState(
  val serverStatus: ServerStatus = ServerStatus.RECONNECTING,
  val serverUrl: String = "http://192.168.0.152:7777",
  val serverEndpoint: String = "/",
  val isPollingActive: Boolean = true,
  val pollingIntervalSeconds: Int = 4,
  val records: List<RecordItem> = emptyList(),
  val filteredRecords: List<RecordItem> = emptyList(),
  val lastSyncTimestamp: Long? = null,
  val lastError: String? = null,
  val isRefreshing: Boolean = false,
  val searchQuery: String = "",
  val selectedFilter: TransactionType? = null,
  val simulationFallbackEnabled: Boolean = true,
  val totalTransactions: Int = 0,
  val totalCredit: Double = 0.0,
  val totalDebit: Double = 0.0,
  val netBalance: Double = 0.0
)

// --- ViewModel ---
class DashboardViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(DashboardUiState())
  val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

  private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .writeTimeout(5, TimeUnit.SECONDS)
    .build()

  private var pollingJob: Job? = null
  private var isAppInForeground = true

  init {
    startPolling()
  }

  fun onAppLifecycleEvent(event: Lifecycle.Event) {
    when (event) {
      Lifecycle.Event.ON_RESUME -> {
        isAppInForeground = true
        if (_uiState.value.isPollingActive && (pollingJob == null || !pollingJob!!.isActive)) {
          startPolling()
        }
      }
      Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
        isAppInForeground = false
        stopPollingJob()
      }
      else -> Unit
    }
  }

  fun togglePolling(enabled: Boolean) {
    _uiState.value = _uiState.value.copy(isPollingActive = enabled)
    if (enabled && isAppInForeground) {
      startPolling()
    } else {
      stopPollingJob()
    }
  }

  fun updateServerSettings(url: String, endpoint: String, interval: Int, simulationFallback: Boolean) {
    val cleanUrl = url.trim().removeSuffix("/")
    val cleanEndpoint = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
    _uiState.value = _uiState.value.copy(
      serverUrl = cleanUrl,
      serverEndpoint = cleanEndpoint,
      pollingIntervalSeconds = interval.coerceIn(2, 60),
      simulationFallbackEnabled = simulationFallback
    )
    refreshData(isManual = true)
  }

  fun setSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
    applyFilters()
  }

  fun setFilter(type: TransactionType?) {
    _uiState.value = _uiState.value.copy(selectedFilter = type)
    applyFilters()
  }

  private fun startPolling() {
    stopPollingJob()
    pollingJob = viewModelScope.launch {
      while (isActive && _uiState.value.isPollingActive && isAppInForeground) {
        fetchRecordsInternal(isManual = false)
        delay(_uiState.value.pollingIntervalSeconds * 1000L)
      }
    }
  }

  private fun stopPollingJob() {
    pollingJob?.cancel()
    pollingJob = null
  }

  fun refreshData(isManual: Boolean = true) {
    viewModelScope.launch {
      fetchRecordsInternal(isManual = isManual)
    }
  }

  private suspend fun fetchRecordsInternal(isManual: Boolean) {
    if (isManual) {
      _uiState.value = _uiState.value.copy(isRefreshing = true)
    }

    val fullUrl = "${_uiState.value.serverUrl}${_uiState.value.serverEndpoint}"

    withContext(Dispatchers.IO) {
      try {
        val request = Request.Builder()
          .url(fullUrl)
          .header("Accept", "application/json, text/html, */*")
          .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
          val parsedItems = parseResponse(responseBody)
          // Strictly sort in descending chronological order (Newest first / الأحدث زمنيًا)
          val sortedItems = parsedItems.sortedByDescending { it.timestamp }

          withContext(Dispatchers.Main) {
            updateRecordsState(sortedItems, ServerStatus.CONNECTED, null)
          }
        } else {
          throw Exception("HTTP ${response.code}: ${response.message}")
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          if (_uiState.value.simulationFallbackEnabled && _uiState.value.records.isEmpty()) {
            // Generate initial live local sample ledger for offline/LAN container demo
            val sampleItems = generateInitialRecords().sortedByDescending { it.timestamp }
            updateRecordsState(
              sampleItems,
              ServerStatus.RECONNECTING,
              "تعذر الاتصال بـ $fullUrl (${e.localizedMessage ?: "الخادم غير متاح"}). يعمل في وضع المعاينة المباشرة."
            )
          } else {
            _uiState.value = _uiState.value.copy(
              serverStatus = ServerStatus.OFFLINE,
              lastError = "فشل الاتصال بـ $fullUrl: ${e.localizedMessage ?: "مهلة الاتصال انتهت"}",
              isRefreshing = false
            )
          }
        }
      }
    }
  }

  private fun parseResponse(body: String): List<RecordItem> {
    val items = mutableListOf<RecordItem>()
    val trimmed = body.trim()

    try {
      if (trimmed.startsWith("[")) {
        val jsonArray = JSONArray(trimmed)
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.optJSONObject(i) ?: continue
          parseJsonObjectToRecord(obj)?.let { items.add(it) }
        }
      } else if (trimmed.startsWith("{")) {
        val jsonObject = JSONObject(trimmed)
        val dataArray = jsonObject.optJSONArray("data")
          ?: jsonObject.optJSONArray("records")
          ?: jsonObject.optJSONArray("transactions")
          ?: jsonObject.optJSONArray("items")

        if (dataArray != null) {
          for (i in 0 until dataArray.length()) {
            val obj = dataArray.optJSONObject(i) ?: continue
            parseJsonObjectToRecord(obj)?.let { items.add(it) }
          }
        } else {
          parseJsonObjectToRecord(jsonObject)?.let { items.add(it) }
        }
      } else {
        // Fallback line parsing if plain text / CSV format
        val lines = trimmed.lines()
        for ((index, line) in lines.withIndex()) {
          if (line.isBlank() || line.startsWith("#")) continue
          val parts = line.split(",", "\t", "|")
          if (parts.size >= 3) {
            val id = "REC-${1000 + index}"
            val title = parts[0].trim()
            val amount = parts[1].trim().toDoubleOrNull() ?: 0.0
            val type = if (parts.size > 3) TransactionType.fromString(parts[3]) else TransactionType.CREDIT
            items.add(
              RecordItem(
                id = id,
                title = title,
                amount = amount,
                timestamp = System.currentTimeMillis() - (index * 120_000L),
                details = if (parts.size > 2) parts[2].trim() else "معاملة حسابية",
                type = type
              )
            )
          }
        }
      }
    } catch (ex: Exception) {
      ex.printStackTrace()
    }

    return items
  }

  private fun parseJsonObjectToRecord(obj: JSONObject): RecordItem? {
    val id = obj.optString("id").ifEmpty {
      obj.optString("_id").ifEmpty { "TXN-${System.currentTimeMillis().toString().takeLast(6)}" }
    }
    val title = obj.optString("title").ifEmpty {
      obj.optString("customerName").ifEmpty {
        obj.optString("customer").ifEmpty {
          obj.optString("name").ifEmpty { "عميل غير محدد" }
        }
      }
    }
    val amount = obj.optDouble("amount", obj.optDouble("value", obj.optDouble("total", 0.0)))
    val details = obj.optString("details").ifEmpty {
      obj.optString("notes").ifEmpty {
        obj.optString("description").ifEmpty { "قيد محاسبي مسجل" }
      }
    }
    val typeStr = obj.optString("type").ifEmpty {
      obj.optString("transactionType").ifEmpty {
        if (amount >= 0) "credit" else "debit"
      }
    }
    val type = TransactionType.fromString(typeStr)

    // Timestamp parsing
    val tsValue = obj.optLong("timestamp", 0L)
    val dateStr = obj.optString("date").ifEmpty { obj.optString("created_at") }
    val timestamp = when {
      tsValue > 0 -> if (tsValue < 1_000_000_000_000L) tsValue * 1000L else tsValue
      dateStr.isNotEmpty() -> parseDateStringToTimestamp(dateStr)
      else -> System.currentTimeMillis()
    }

    return RecordItem(
      id = id,
      title = title,
      amount = Math.abs(amount),
      timestamp = timestamp,
      details = details,
      type = type,
      rawDateString = dateStr
    )
  }

  private fun parseDateStringToTimestamp(dateStr: String): Long {
    val patterns = arrayOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd HH:mm:ss",
      "yyyy-MM-dd",
      "dd/MM/yyyy HH:mm:ss",
      "dd/MM/yyyy"
    )
    for (p in patterns) {
      try {
        val sdf = SimpleDateFormat(p, Locale.US)
        val date = sdf.parse(dateStr)
        if (date != null) return date.time
      } catch (_: Exception) {}
    }
    return System.currentTimeMillis()
  }

  private fun updateRecordsState(items: List<RecordItem>, status: ServerStatus, error: String?) {
    val totalCount = items.size
    val totalCred = items.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
    val totalDeb = items.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
    val net = totalCred - totalDeb

    _uiState.value = _uiState.value.copy(
      serverStatus = status,
      records = items,
      lastSyncTimestamp = System.currentTimeMillis(),
      lastError = error,
      isRefreshing = false,
      totalTransactions = totalCount,
      totalCredit = totalCred,
      totalDebit = totalDeb,
      netBalance = net
    )
    applyFilters()
  }

  private fun applyFilters() {
    val current = _uiState.value
    val query = current.searchQuery.trim().lowercase()
    val filterType = current.selectedFilter

    val result = current.records.filter { item ->
      val matchesSearch = query.isEmpty() ||
        item.title.lowercase().contains(query) ||
        item.details.lowercase().contains(query) ||
        item.id.lowercase().contains(query) ||
        item.amount.toString().contains(query)

      val matchesType = filterType == null || item.type == filterType

      matchesSearch && matchesType
    }

    _uiState.value = _uiState.value.copy(filteredRecords = result)
  }

  private fun generateInitialRecords(): List<RecordItem> {
    val now = System.currentTimeMillis()
    return listOf(
      RecordItem(
        id = "INV-2026-094",
        title = "مؤسسة النور للتجارة العامة",
        amount = 14500.00,
        timestamp = now - (2 * 60 * 1000L),
        details = "دفعة حساب توريدات معدات محاسبية ومستندات صرف رقم 8812",
        type = TransactionType.CREDIT
      ),
      RecordItem(
        id = "REC-88412",
        title = "شركة الوفاق اللوجستية",
        amount = 3200.50,
        timestamp = now - (15 * 60 * 1000L),
        details = "سداد فاتورة شحن ونقل بضائع مستودعات الفرع الرئيسي",
        type = TransactionType.DEBIT
      ),
      RecordItem(
        id = "TXN-55102",
        title = "سالم بن عبد الله الثبياني",
        amount = 8750.00,
        timestamp = now - (45 * 60 * 1000L),
        details = "تحصيل إيراد خدمات استشارية وعقد الدعم الفني الشهري",
        type = TransactionType.CREDIT
      ),
      RecordItem(
        id = "BIL-10294",
        title = "مجموعة الصفا والمروة للمقاولات",
        amount = 22400.00,
        timestamp = now - (2 * 3600 * 1000L),
        details = "إيداع شيك مستحقات مشروع تطوير المنظومة السحابية",
        type = TransactionType.CREDIT
      ),
      RecordItem(
        id = "VOUCH-3310",
        title = "شركة الاتصالات والربط الشبكي",
        amount = 1850.00,
        timestamp = now - (5 * 3600 * 1000L),
        details = "مصروف اشتراك خطوط الألياف الضوئية والخوادم المخصصة",
        type = TransactionType.DEBIT
      ),
      RecordItem(
        id = "TR-90021",
        title = "مركز الريادة للأجهزة والتقنية",
        amount = 4600.00,
        timestamp = now - (9 * 3600 * 1000L),
        details = "سداد قيمة أجهزة نقط البيع والماسحات الضوئية",
        type = TransactionType.DEBIT
      )
    )
  }

  fun injectSimulatedEntry() {
    val now = System.currentTimeMillis()
    val titles = listOf(
      "مكتب الأفق للاستشارات المالية",
      "مجمع الأندلس التجاري",
      "مستودعات الخليج المركزية",
      "المهندس أحمد الزهراني"
    )
    val types = listOf(TransactionType.CREDIT, TransactionType.DEBIT)
    val randomType = types.random()
    val randomAmount = (500..15000 step 250).toList().random().toDouble()

    val newRecord = RecordItem(
      id = "TXN-${System.currentTimeMillis().toString().takeLast(5)}",
      title = titles.random(),
      amount = randomAmount,
      timestamp = now,
      details = "قيد مباشر مسجل عبر الربط اللحظي للخادم المحلي",
      type = randomType
    )

    val updatedList = (listOf(newRecord) + _uiState.value.records).sortedByDescending { it.timestamp }
    updateRecordsState(updatedList, ServerStatus.CONNECTED, null)
  }
}

// --- Main Activity ---
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          LiveAccountingScreen()
        }
      }
    }
  }
}

// Backward-compatible Greeting for Tests
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

// --- Screens & Components ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAccountingScreen(viewModel: DashboardViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsState()
  val lifecycleOwner = LocalLifecycleOwner.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var showSettingsDialog by remember { mutableStateOf(false) }
  var showFlutterCodeDialog by remember { mutableStateOf(false) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      viewModel.onAppLifecycleEvent(event)
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "polling_pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
          Column {
            Text(
              text = "استعراض البيانات المباشر",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("app_title")
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(
                    if (uiState.serverStatus == ServerStatus.RECONNECTING) {
                      uiState.serverStatus.color.copy(alpha = pulseAlpha)
                    } else {
                      uiState.serverStatus.color
                    }
                  )
              )
              Text(
                text = "${uiState.serverUrl} • ${uiState.serverStatus.labelArabic}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          // Manual Refresh Button
          IconButton(
            onClick = { viewModel.refreshData(isManual = true) },
            enabled = !uiState.isRefreshing,
            modifier = Modifier.testTag("refresh_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "تحديث يدوي",
              modifier = if (uiState.isRefreshing) Modifier.rotate(pulseAlpha * 360f) else Modifier
            )
          }

          // Code Deliverables Viewer Button
          IconButton(
            onClick = { showFlutterCodeDialog = true },
            modifier = Modifier.testTag("flutter_code_button")
          ) {
            Icon(
              imageVector = Icons.Default.Code,
              contentDescription = "كود Flutter و pubspec.yaml"
            )
          }

          // Settings Button
          IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier.testTag("settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "إعدادات الخادم"
            )
          }
        }
      )
    },
    containerColor = MaterialTheme.colorScheme.background
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Server Status & Polling Bar
      ServerStatusBar(
        serverStatus = uiState.serverStatus,
        serverUrl = "${uiState.serverUrl}${uiState.serverEndpoint}",
        isPolling = uiState.isPollingActive,
        lastSyncTimestamp = uiState.lastSyncTimestamp,
        onTogglePolling = { viewModel.togglePolling(it) },
        onInjectTestRecord = { viewModel.injectSimulatedEntry() }
      )

      // Warning/Info banner if simulated or error occurred
      if (uiState.lastError != null) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          colors = CardDefaults.cardColors(
            containerColor = Amber500.copy(alpha = 0.12f)
          ),
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = Amber500,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = uiState.lastError ?: "",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // Summary Dashboard Card
      DashboardSummaryCard(
        totalTransactions = uiState.totalTransactions,
        totalCredit = uiState.totalCredit,
        totalDebit = uiState.totalDebit,
        netBalance = uiState.netBalance,
        lastSyncTimestamp = uiState.lastSyncTimestamp
      )

      // Search & Filters
      SearchAndFilterSection(
        searchQuery = uiState.searchQuery,
        onSearchChanged = { viewModel.setSearchQuery(it) },
        selectedFilter = uiState.selectedFilter,
        onFilterSelected = { viewModel.setFilter(it) }
      )

      // Records List / Empty State
      if (uiState.filteredRecords.isEmpty()) {
        EmptyStateView(
          isOffline = uiState.serverStatus == ServerStatus.OFFLINE,
          isSearching = uiState.searchQuery.isNotEmpty() || uiState.selectedFilter != null,
          onRetry = { viewModel.refreshData(isManual = true) }
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("records_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "السجلات الأخيرة (مرتبة حسب الأحدث زمنيًا)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${uiState.filteredRecords.size} سجل",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          items(
            items = uiState.filteredRecords,
            key = { it.id }
          ) { record ->
            RecordItemCard(record = record)
          }

          item {
            Spacer(modifier = Modifier.height(24.dp))
          }
        }
      }
    }
  }

  // Server Settings Dialog
  if (showSettingsDialog) {
    ServerSettingsDialog(
      currentUrl = uiState.serverUrl,
      currentEndpoint = uiState.serverEndpoint,
      currentInterval = uiState.pollingIntervalSeconds,
      simulationEnabled = uiState.simulationFallbackEnabled,
      onDismiss = { showSettingsDialog = false },
      onSave = { url, ep, interval, sim ->
        viewModel.updateServerSettings(url, ep, interval, sim)
        showSettingsDialog = false
      }
    )
  }

  // Flutter Code & Deliverables Dialog
  if (showFlutterCodeDialog) {
    FlutterDeliverablesDialog(
      onDismiss = { showFlutterCodeDialog = false }
    )
  }
}

// --- Sub-Composables ---

@Composable
fun ServerStatusBar(
  serverStatus: ServerStatus,
  serverUrl: String,
  isPolling: Boolean,
  lastSyncTimestamp: Long?,
  onTogglePolling: (Boolean) -> Unit,
  onInjectTestRecord: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = serverStatus.color.copy(alpha = 0.15f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(serverStatus.color)
            )
            Text(
              text = serverStatus.labelArabic,
              style = MaterialTheme.typography.labelMedium,
              color = serverStatus.color,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Column {
          Text(
            text = "تزامن الخادم اللحظي",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = if (isPolling) "الفحص الدوري كل 3-5 ثوانٍ" else "الفحص التلقائي متوقف",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onInjectTestRecord,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "إضافة قيد تجريبي مباشر",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
          checked = isPolling,
          onCheckedChange = onTogglePolling,
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Emerald600
          ),
          modifier = Modifier.testTag("polling_switch")
        )
      }
    }
  }
}

@Composable
fun DashboardSummaryCard(
  totalTransactions: Int,
  totalCredit: Double,
  totalDebit: Double,
  netBalance: Double,
  lastSyncTimestamp: Long?
) {
  val currencyFormat = remember { DecimalFormat("#,##0.00") }
  val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale("ar")) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = Slate900
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "ملخص العمليات الحسابية",
          style = MaterialTheme.typography.titleSmall,
          color = Slate100,
          fontWeight = FontWeight.Bold
        )

        val syncText = if (lastSyncTimestamp != null) {
          "آخر تحديث: ${timeFormat.format(Date(lastSyncTimestamp))}"
        } else {
          "في انتظار أول مزامنة..."
        }

        Text(
          text = syncText,
          style = MaterialTheme.typography.labelSmall,
          color = Slate100.copy(alpha = 0.7f)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Total Count
        SummaryItem(
          label = "إجمالي السجلات",
          value = "$totalTransactions",
          valueColor = Slate50,
          icon = Icons.Default.Sync
        )

        // Total Credit (دائن / إيداع)
        SummaryItem(
          label = "إجمالي الدائن (+)",
          value = currencyFormat.format(totalCredit),
          valueColor = Emerald500,
          icon = Icons.Default.ArrowDownward
        )

        // Total Debit (مدين / صرف)
        SummaryItem(
          label = "إجمالي المدين (-)",
          value = currencyFormat.format(totalDebit),
          valueColor = Rose500,
          icon = Icons.Default.ArrowUpward
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Net Balance Bar
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate800
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "الرصيد الصافي المجمع:",
            style = MaterialTheme.typography.bodySmall,
            color = Slate100
          )
          Text(
            text = "${currencyFormat.format(netBalance)} ر.س",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (netBalance >= 0) Emerald500 else Rose500
          )
        }
      }
    }
  }
}

@Composable
fun SummaryItem(
  label: String,
  value: String,
  valueColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector
) {
  Column {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = valueColor,
        modifier = Modifier.size(14.dp)
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Slate100.copy(alpha = 0.7f)
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = valueColor
    )
  }
}

@Composable
fun SearchAndFilterSection(
  searchQuery: String,
  onSearchChanged: (String) -> Unit,
  selectedFilter: TransactionType?,
  onFilterSelected: (TransactionType?) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChanged,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("search_field"),
      placeholder = { Text("بحث باسم العميل، الملاحظات، أو رقم القيد...") },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "بحث"
        )
      },
      shape = RoundedCornerShape(14.dp),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = selectedFilter == null,
        onClick = { onFilterSelected(null) },
        label = { Text("الكل") },
        shape = RoundedCornerShape(10.dp)
      )

      FilterChip(
        selected = selectedFilter == TransactionType.CREDIT,
        onClick = { onFilterSelected(TransactionType.CREDIT) },
        label = { Text("دائن / إيداع") },
        leadingIcon = {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(Emerald500)
          )
        },
        shape = RoundedCornerShape(10.dp)
      )

      FilterChip(
        selected = selectedFilter == TransactionType.DEBIT,
        onClick = { onFilterSelected(TransactionType.DEBIT) },
        label = { Text("مدين / صرف") },
        leadingIcon = {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(Rose500)
          )
        },
        shape = RoundedCornerShape(10.dp)
      )
    }
  }
}

@Composable
fun RecordItemCard(record: RecordItem) {
  var expanded by remember { mutableStateOf(false) }
  val currencyFormat = remember { DecimalFormat("#,##0.00") }
  val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd  hh:mm a", Locale("ar")) }

  val relativeTime = remember(record.timestamp) {
    val diff = System.currentTimeMillis() - record.timestamp
    val minutes = diff / 60000
    val hours = diff / 3600000
    when {
      minutes < 1 -> "الآن / مباشر"
      minutes < 60 -> "منذ $minutes دقيقة"
      hours < 24 -> "منذ $hours ساعة"
      else -> "${diff / 86400000} يوم"
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { expanded = !expanded }
      .testTag("record_item_${record.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = record.id,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontFamily = FontFamily.Monospace
              )
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (record.type == TransactionType.CREDIT) Emerald500.copy(alpha = 0.15f) else Rose500.copy(alpha = 0.15f)
            ) {
              Text(
                text = record.type.labelArabic,
                style = MaterialTheme.typography.labelSmall,
                color = if (record.type == TransactionType.CREDIT) Emerald600 else Rose600,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = record.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Column(
          horizontalAlignment = Alignment.End
        ) {
          Text(
            text = "${if (record.type == TransactionType.CREDIT) "+" else "-"}${currencyFormat.format(record.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (record.type == TransactionType.CREDIT) Emerald600 else Rose600
          )
          Text(
            text = "ريال سعودي",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = dateFormat.format(Date(record.timestamp)),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
          Text(
            text = relativeTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontWeight = FontWeight.Medium
          )
        }
      }

      AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "البيان والتفاصيل المحاسبية:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = record.details.ifEmpty { "لا توجد تفاصيل إضافية لهذا القيد" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun EmptyStateView(
  isOffline: Boolean,
  isSearching: Boolean,
  onRetry: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Icon(
      imageVector = if (isOffline) Icons.Default.ErrorOutline else Icons.Default.Search,
      contentDescription = null,
      tint = if (isOffline) Rose500 else MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(64.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = if (isOffline) "تعذر الاتصال بخادم الحسابات المحلي" else if (isSearching) "لا توجد نتائج مطابقة لبحثك" else "لا توجد قيود مسجلة حالياً",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = if (isOffline) "تأكد من تشغيل الخادم على 192.168.0.152:7777 وتفعيل الاتصال على نفس الشبكة المحلية." else "سيتم تحديث القائمة فور وصول قيود جديدة من الخادم.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
      onClick = onRetry,
      shape = RoundedCornerShape(12.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = null,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("إعادة المحاولة")
    }
  }
}

@Composable
fun ServerSettingsDialog(
  currentUrl: String,
  currentEndpoint: String,
  currentInterval: Int,
  simulationEnabled: Boolean,
  onDismiss: () -> Unit,
  onSave: (url: String, endpoint: String, interval: Int, simulation: Boolean) -> Unit
) {
  var urlInput by remember { mutableStateOf(currentUrl) }
  var endpointInput by remember { mutableStateOf(currentEndpoint) }
  var intervalInput by remember { mutableStateOf(currentInterval.toString()) }
  var simEnabled by remember { mutableStateOf(simulationEnabled) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "إعدادات الاتصال بالخادم",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = urlInput,
          onValueChange = { urlInput = it },
          label = { Text("عنوان الخادم (Base URL)") },
          placeholder = { Text("http://192.168.0.152:7777") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = endpointInput,
          onValueChange = { endpointInput = it },
          label = { Text("المسار (Endpoint)") },
          placeholder = { Text("/ أو /api/data") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = intervalInput,
          onValueChange = { intervalInput = it },
          label = { Text("فترة التحديث بالثواني (3-5)") },
          placeholder = { Text("4") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "تفعيل المعاينة اللحظية",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "توليد قيود عند تعذر الوصول لشبكة LAN",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = simEnabled,
            onCheckedChange = { simEnabled = it }
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val interval = intervalInput.toIntOrNull() ?: 4
          onSave(urlInput, endpointInput, interval, simEnabled)
        }
      ) {
        Text("حفظ وتطبيق")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء")
      }
    }
  )
}

@Composable
fun FlutterDeliverablesDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableStateOf(0) }
  val tabs = listOf("main.dart (كامل)", "pubspec.yaml", "إعدادات Android")

  val flutterCode = remember {
    """
import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:http/http.dart' as http;
import 'package:intl/intl.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const LiveAccountingApp());
}

class LiveAccountingApp extends StatelessWidget {
  const LiveAccountingApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'استعراض البيانات المباشر',
      debugShowCheckedModeBanner: false,
      locale: const Locale('ar'),
      supportedLocales: const [Locale('ar')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF4F46E5),
        brightness: Brightness.light,
        scaffoldBackgroundColor: const Color(0xFFF8FAFC),
        fontFamily: 'Roboto',
      ),
      home: const Directionality(
        textDirection: TextDirection.rtl,
        child: LiveDashboardScreen(),
      ),
    );
  }
}

enum ServerStatus { connected, reconnecting, offline }

enum TransactionType { credit, debit }

class RecordItem {
  final String id;
  final String title;
  final double amount;
  final DateTime timestamp;
  final String details;
  final TransactionType type;

  RecordItem({
    required this.id,
    required this.title,
    required this.amount,
    required this.timestamp,
    required this.details,
    required this.type,
  });

  factory RecordItem.fromJson(Map<String, dynamic> json) {
    final id = (json['id'] ?? json['_id'] ?? 'REC-${'$'}{DateTime.now().millisecondsSinceEpoch}').toString();
    final title = (json['title'] ?? json['customerName'] ?? json['customer'] ?? json['name'] ?? 'عميل غير محدد').toString();
    final amount = double.tryParse((json['amount'] ?? json['value'] ?? json['total'] ?? 0).toString()) ?? 0.0;
    final details = (json['details'] ?? json['notes'] ?? json['description'] ?? 'قيد محاسبي مسجل').toString();
    
    final typeStr = (json['type'] ?? json['transactionType'] ?? 'credit').toString().toLowerCase();
    final type = (typeStr.contains('credit') || typeStr.contains('إيداع') || typeStr.contains('له') || typeStr.contains('قبض'))
        ? TransactionType.credit
        : TransactionType.debit;

    DateTime date;
    if (json['timestamp'] != null) {
      final ts = int.tryParse(json['timestamp'].toString()) ?? 0;
      date = ts > 0 
        ? DateTime.fromMillisecondsSinceEpoch(ts < 1000000000000 ? ts * 1000 : ts) 
        : DateTime.now();
    } else if (json['date'] != null || json['created_at'] != null) {
      date = DateTime.tryParse((json['date'] ?? json['created_at']).toString()) ?? DateTime.now();
    } else {
      date = DateTime.now();
    }

    return RecordItem(
      id: id,
      title: title,
      amount: amount.abs(),
      timestamp: date,
      details: details,
      type: type,
    );
  }
}

class LiveDashboardScreen extends StatefulWidget {
  const LiveDashboardScreen({super.key});

  @override
  State<LiveDashboardScreen> createState() => _LiveDashboardScreenState();
}

class _LiveDashboardScreenState extends State<LiveDashboardScreen> with WidgetsBindingObserver {
  final String _baseUrl = 'http://192.168.0.152:7777';
  final String _endpoint = '/';
  
  ServerStatus _status = ServerStatus.reconnecting;
  bool _isAutoSyncEnabled = true;
  bool _isLoading = false;
  Timer? _pollingTimer;
  DateTime? _lastSyncTime;
  List<RecordItem> _records = [];
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _fetchData();
    _startPolling();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _stopPolling();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      if (_isAutoSyncEnabled) _startPolling();
    } else if (state == AppLifecycleState.paused || state == AppLifecycleState.inactive) {
      _stopPolling();
    }
  }

  void _startPolling() {
    _stopPolling();
    _pollingTimer = Timer.periodic(const Duration(seconds: 4), (timer) {
      if (_isAutoSyncEnabled && mounted) {
        _fetchData(isSilent: true);
      }
    });
  }

  void _stopPolling() {
    _pollingTimer?.cancel();
    _pollingTimer = null;
  }

  Future<void> _fetchData({bool isSilent = false}) async {
    if (!isSilent) {
      setState(() => _isLoading = true);
    }

    final url = Uri.parse('${'$'}_baseUrl${'$'}_endpoint');
    try {
      final response = await http.get(url).timeout(const Duration(seconds: 5));
      if (response.statusCode == 200) {
        final dynamic decoded = jsonDecode(response.body);
        List<dynamic> rawList = [];

        if (decoded is List) {
          rawList = decoded;
        } else if (decoded is Map<String, dynamic>) {
          rawList = decoded['data'] ?? decoded['records'] ?? decoded['transactions'] ?? [decoded];
        }

        final items = rawList.map((e) => RecordItem.fromJson(Map<String, dynamic>.from(e))).toList();
        
        // Strictly sort in descending chronological order (Newest entries first)
        items.sort((a, b) => b.timestamp.compareTo(a.timestamp));

        if (mounted) {
          setState(() {
            _records = items;
            _status = ServerStatus.connected;
            _lastSyncTime = DateTime.now();
            _errorMessage = null;
            _isLoading = false;
          });
        }
      } else {
        throw Exception('HTTP Error ${'$'}{response.statusCode}');
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _status = _records.isEmpty ? ServerStatus.offline : ServerStatus.reconnecting;
          _errorMessage = 'فشل الاتصال بالخادم (${'$'}_baseUrl)';
          _isLoading = false;
        });
      }
    }
  }

  Color _getStatusColor() {
    switch (_status) {
      case ServerStatus.connected: return const Color(0xFF10B981);
      case ServerStatus.reconnecting: return const Color(0xFFF59E0B);
      case ServerStatus.offline: return const Color(0xFFEF4444);
    }
  }

  String _getStatusText() {
    switch (_status) {
      case ServerStatus.connected: return 'متصل';
      case ServerStatus.reconnecting: return 'جاري الاتصال';
      case ServerStatus.offline: return 'غير متصل';
    }
  }

  @override
  Widget build(BuildContext context) {
    final currencyFormat = NumberFormat('#,##0.00', 'ar');
    final timeFormat = DateFormat('hh:mm:ss a', 'ar');

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 1,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'استعراض البيانات المباشر',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Color(0xFF0F172A)),
            ),
            Row(
              children: [
                Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(shape: BoxShape.circle, color: _getStatusColor()),
                ),
                const SizedBox(width: 6),
                Text(
                  '192.168.0.152:7777 • ${'$'}{_getStatusText()}',
                  style: const TextStyle(fontSize: 12, color: Color(0xFF64748B)),
                ),
              ],
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: _isLoading
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.refresh, color: Color(0xFF4F46E5)),
            onPressed: () => _fetchData(),
          ),
          Switch(
            value: _isAutoSyncEnabled,
            activeColor: const Color(0xFF10B981),
            onChanged: (val) {
              setState(() => _isAutoSyncEnabled = val);
              if (val) _startPolling(); else _stopPolling();
            },
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () => _fetchData(),
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Dashboard Summary Card
            Card(
              color: const Color(0xFF0F172A),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'ملخص الحركات والعمليات',
                          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14),
                        ),
                        Text(
                          _lastSyncTime != null ? 'آخر تحديث: ${'$'}{timeFormat.format(_lastSyncTime!)}' : 'في انتظار البيانات...',
                          style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 12),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        _buildMetric('إجمالي العمليات', '${'$'}{_records.length}', Colors.white),
                        _buildMetric(
                          'إجمالي الدائن (+)',
                          currencyFormat.format(_records.where((e) => e.type == TransactionType.credit).fold(0.0, (s, e) => s + e.amount)),
                          const Color(0xFF10B981),
                        ),
                        _buildMetric(
                          'إجمالي المدين (-)',
                          currencyFormat.format(_records.where((e) => e.type == TransactionType.debit).fold(0.0, (s, e) => s + e.amount)),
                          const Color(0xFFEF4444),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Section Title
            const Text(
              'السجلات الحديثة (الأحدث زمنيًا):',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Color(0xFF334155)),
            ),
            const SizedBox(height: 8),

            // Records List or Empty/Error state
            if (_records.isEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 40),
                child: Column(
                  children: [
                    Icon(
                      _status == ServerStatus.offline ? Icons.wifi_off : Icons.inbox,
                      size: 64,
                      color: const Color(0xFF94A3B8),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      _status == ServerStatus.offline
                          ? 'تعذر الاتصال بالخادم المحلي (192.168.0.152:7777)'
                          : 'لا توجد بيانات متاحة حالياً',
                      textAlign: TextAlign.center,
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF334155)),
                    ),
                    const SizedBox(height: 16),
                    ElevatedButton.icon(
                      onPressed: () => _fetchData(),
                      icon: const Icon(Icons.replay),
                      label: const Text('إعادة المحاولة'),
                    ),
                  ],
                ),
              )
            else
              ..._records.map((record) => _buildRecordCard(record, currencyFormat, timeFormat)),
          ],
        ),
      ),
    );
  }

  Widget _buildMetric(String label, String value, Color color) {
    return Column(
      children: [
        Text(label, style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 11)),
        const SizedBox(height: 4),
        Text(value, style: TextStyle(color: color, fontSize: 16, fontWeight: FontWeight.bold)),
      ],
    );
  }

  Widget _buildRecordCard(RecordItem record, NumberFormat currencyFormat, DateFormat timeFormat) {
    final isCredit = record.type == TransactionType.credit;
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      elevation: 1.5,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF1F5F9),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(record.id, style: const TextStyle(fontSize: 11, color: Color(0xFF475569))),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: isCredit ? const Color(0xFFD1FAE5) : const Color(0xFFFFE4E6),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        isCredit ? 'دائن / إيداع' : 'مدين / صرف',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          color: isCredit ? const Color(0xFF059669) : const Color(0xFFE11D48),
                        ),
                      ),
                    ),
                  ],
                ),
                Text(
                  (isCredit ? "+" : "-") + currencyFormat.format(record.amount) + " ر.س",
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: isCredit ? const Color(0xFF059669) : const Color(0xFFE11D48),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              record.title,
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Color(0xFF0F172A)),
            ),
            const SizedBox(height: 4),
            Text(
              record.details,
              style: const TextStyle(fontSize: 13, color: Color(0xFF64748B)),
            ),
            const Divider(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  DateFormat('yyyy/MM/dd  hh:mm a', 'ar').format(record.timestamp),
                  style: const TextStyle(fontSize: 12, color: Color(0xFF94A3B8)),
                ),
                const Text('مؤرشف وموثق', style: TextStyle(fontSize: 11, color: Color(0xFF4F46E5))),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
"""
  }

  val pubspecYaml = remember {
    """name: live_accounting_monitor
description: "A real-time accounting dashboard connecting to local HTTP server."
publish_to: 'none'
version: 1.0.0+1

environment:
  sdk: '>=3.0.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter
  flutter_localizations:
    sdk: flutter
  http: ^1.2.0
  intl: ^0.19.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0

flutter:
  uses-material-design: true
"""
  }

  val androidManifestInstructions = remember {
    """<!-- في ملف android/app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 1. تفعيل صلاحيات الإنترنت والشبكة -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:label="استعراض البيانات"
        android:name="${'$'}{applicationName}"
        android:icon="@mipmap/ic_launcher"
        <!-- 2. تفعيل بروتوكول HTTP غير المشفر للاتصال بالخادم المحلي 192.168.0.152 -->
        android:usesCleartextTraffic="true">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:theme="@style/LaunchTheme"
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
            android:hardwareAccelerated="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
"""
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Code,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Text("مخرجات كود Flutter و pubspec")
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        ScrollableTabRow(
          selectedTabIndex = selectedTab,
          edgePadding = 0.dp
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = { Text(title, maxLines = 1) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val currentContent = when (selectedTab) {
          0 -> flutterCode
          1 -> pubspecYaml
          else -> androidManifestInstructions
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Slate900, RoundedCornerShape(10.dp))
            .padding(10.dp)
        ) {
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
              Text(
                text = currentContent,
                color = Slate100,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val textToCopy = when (selectedTab) {
            0 -> flutterCode
            1 -> pubspecYaml
            else -> androidManifestInstructions
          }
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          val clip = ClipData.newPlainText("Deliverable Code", textToCopy)
          clipboard.setPrimaryClip(clip)
          Toast.makeText(context, "تم نسخ النص إلى الحافظة بنجاح", Toast.LENGTH_SHORT).show()
        }
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("نسخ التبويب الحالي")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إغلاق")
      }
    }
  )
}
