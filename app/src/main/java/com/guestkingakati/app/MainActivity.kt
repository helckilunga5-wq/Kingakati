package com.guestkingakati.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal val Gold = Color(0xFFE1B85B)
internal val Dark = Color(0xFF11110E)
internal val Panel = Color(0xFF1D1C17)
internal val Cream = Color(0xFFF8F5EC)

const val WHATSAPP_NUMBER = "243834989934"

// Collez ici le lien CSV de votre Google Sheet publié (voir LISEZ-MOI.md).
// Tant que ce lien est vide, l'application affiche les données de secours ci-dessous.
const val SHEET_CSV_URL = ""

internal val ONLINE_ENABLED = SHEET_CSV_URL.isNotBlank()

/* ------------------------------------------------------------------ */
/*  Catalogue (chambres, plats, boissons) lu depuis Google Sheets      */
/* ------------------------------------------------------------------ */

data class CatalogItem(
    val category: String,   // Chambre, Plat ou Boisson
    val name: String,
    val description: String,
    val priceUsd: String,
    val priceFc: String,
    val photoUrl: String,
    val available: Boolean
)

object Catalog {
    private const val PREFS = "guest_kingakati"
    private const val KEY = "catalog_csv"

    // Données de secours (hors connexion, ou lien non configuré)
    val fallback = listOf(
        CatalogItem(
            category = "Chambre",
            name = "Chambre climatisée",
            description = "Chambre climatisée avec petit déjeuner.",
            priceUsd = "60",
            priceFc = "145000",
            photoUrl = "",
            available = true
        )
    )

    fun cached(context: Context): List<CatalogItem>? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return null
        return parse(raw).takeIf { it.isNotEmpty() }
    }

    suspend fun fetch(context: Context): List<CatalogItem>? {
        if (SHEET_CSV_URL.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(SHEET_CSV_URL).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                val text = try {
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } finally {
                    connection.disconnect()
                }
                val items = parse(text)
                if (items.isEmpty()) {
                    null
                } else {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY, text)
                        .apply()
                    items
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    // Colonnes : categorie, nom, description, prix_usd, prix_fc, photo_url, disponible
    fun parse(csv: String): List<CatalogItem> {
        return parseCsv(csv).drop(1).mapNotNull { row ->
            fun col(i: Int): String = row.getOrNull(i)?.trim().orEmpty()

            val name = col(1)
            if (name.isEmpty()) return@mapNotNull null

            val dispo = col(6).lowercase(Locale.ROOT)
            CatalogItem(
                category = col(0),
                name = name,
                description = col(2),
                priceUsd = col(3),
                priceFc = col(4),
                photoUrl = col(5),
                available = dispo !in listOf("non", "no", "0", "false", "faux")
            )
        }
    }
}

fun parseCsv(text: String): List<List<String>> {
    val s = text.removePrefix("\uFEFF")
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var i = 0

    while (i < s.length) {
        val c = s[i]
        if (inQuotes) {
            if (c == '"' && i + 1 < s.length && s[i + 1] == '"') {
                field.append('"')
                i++
            } else if (c == '"') {
                inQuotes = false
            } else {
                field.append(c)
            }
        } else if (c == '"') {
            inQuotes = true
        } else if (c == ',') {
            row.add(field.toString())
            field.setLength(0)
        } else if (c == '\n' || c == '\r') {
            if (c == '\r' && i + 1 < s.length && s[i + 1] == '\n') i++
            row.add(field.toString())
            field.setLength(0)
            if (row.any { it.isNotBlank() }) rows.add(row)
            row = mutableListOf()
        } else {
            field.append(c)
        }
        i++
    }

    row.add(field.toString())
    if (row.any { it.isNotBlank() }) rows.add(row)
    return rows
}

fun formatPrice(usd: String, fc: String): String {
    val parts = mutableListOf<String>()

    val usdClean = usd.trim().removeSuffix("$").trim()
    if (usdClean.isNotEmpty()) parts.add("$usdClean $")

    val fcDigits = fc.filter { it.isDigit() }.toLongOrNull()
    if (fcDigits != null) {
        parts.add(String.format(Locale.FRANCE, "%,d", fcDigits) + " FC")
    } else if (fc.isNotBlank()) {
        parts.add(fc.trim())
    }

    return if (parts.isEmpty()) "Tarif à confirmer" else parts.joinToString(" ou ")
}

/* ------------------------------------------------------------------ */
/*  Activité                                                           */
/* ------------------------------------------------------------------ */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuestKingakatiApp()
        }
    }
}

internal val navItems = listOf(
    "Accueil" to "🏠",
    "Hôtel" to "🛏️",
    "Restaurant" to "🍽️",
    "Réserver" to "📅",
    "Mes demandes" to "📋"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestKingakatiApp() {
    val context = LocalContext.current

    var page by rememberSaveable { mutableStateOf("Accueil") }
    var presetService by rememberSaveable { mutableStateOf("Chambre") }

    var catalog by remember { mutableStateOf(Catalog.cached(context) ?: Catalog.fallback) }
    var loading by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        Catalog.fetch(context)?.let { catalog = it }
        loading = false
    }

    fun go(target: String, service: String? = null) {
        if (service != null) presetService = service
        page = target
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            background = Dark,
            surface = Panel,
            onPrimary = Dark,
            onBackground = Cream,
            onSurface = Cream
        )
    ) {
        Scaffold(
            containerColor = Dark,
            contentColor = Cream,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    navItems.forEach { (label, emoji) ->
                        NavigationBarItem(
                            selected = page == label,
                            onClick = { page = label },
                            icon = { Text(emoji, fontSize = 20.sp) },
                            label = { Text(label, fontSize = 10.sp, maxLines = 1) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    "GUEST KINGAKATI",
                    color = Gold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "SÉJOURNEZ • SAVOUREZ • CÉLÉBREZ",
                    color = Cream,
                    fontSize = 10.sp
                )

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    when (page) {
                        "Accueil" -> HomePage(
                            onNavigate = { target, service -> go(target, service) }
                        )
                        "Hôtel" -> HotelPage(
                            rooms = catalog.filter {
                                it.category.equals("Chambre", ignoreCase = true) && it.available
                            },
                            loading = loading,
                            onRefresh = { refreshKey++ },
                            onReserve = { go("Réserver", "Chambre") }
                        )
                        "Restaurant" -> RestaurantPage(
                            dishes = catalog.filter {
                                it.category.equals("Plat", ignoreCase = true) && it.available
                            },
                            drinks = catalog.filter {
                                it.category.equals("Boisson", ignoreCase = true) && it.available
                            },
                            loading = loading,
                            onRefresh = { refreshKey++ },
                            onReserve = { go("Réserver", "Restaurant") }
                        )
                        "Réserver" -> BookingPage(initialService = presetService)
                        "Mes demandes" -> HistoryPage()
                    }
                }
            }
        }
    }
}
