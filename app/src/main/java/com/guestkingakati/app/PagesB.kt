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
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingPage(initialService: String) {
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var service by rememberSaveable(initialService) { mutableStateOf(initialService) }
    var date by rememberSaveable { mutableStateOf("") }
    var guests by rememberSaveable { mutableStateOf("1") }
    var details by rememberSaveable { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    val phoneOk = isValidPhone(phone)
    val dateOk = isValidDate(date)
    val guestsOk = guests.toIntOrNull()?.let { it in 1..500 } == true
    val formOk = name.isNotBlank() && phoneOk && dateOk && guestsOk

    if (showPicker) {
        val local = Calendar.getInstance()
        val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val pickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= todayUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val f = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                            f.timeZone = TimeZone.getTimeZone("UTC")
                            date = f.format(Date(millis))
                        }
                        showPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Demande de réservation", color = Gold, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Votre demande sera envoyée par WhatsApp. " +
                    "Elle sera confirmée après réponse de l'établissement.",
                color = Cream
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom complet") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = phone,
                onValueChange = { v ->
                    if (v.length <= 20 && v.all { it.isDigit() || it in "+ -()" }) phone = v
                },
                label = { Text("Téléphone") },
                placeholder = { Text("+243 ...") },
                singleLine = true,
                isError = phone.isNotEmpty() && !phoneOk,
                supportingText = if (phone.isNotEmpty() && !phoneOk) {
                    { Text("Numéro invalide (9 à 15 chiffres).") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Service", color = Gold)
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    "Chambre",
                    "Restaurant",
                    "Mariage",
                    "Anniversaire",
                    "Autre événement",
                    "Loisirs"
                ).forEach {
                    FilterChip(
                        selected = service == it,
                        onClick = { service = it },
                        label = { Text(it) }
                    )
                }
            }
        }

        item {
            Text("Date souhaitée", color = Gold)
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (date.isBlank()) "📅 Choisir la date" else "📅 $date")
            }
        }

        item {
            OutlinedTextField(
                value = guests,
                onValueChange = { v ->
                    if (v.length <= 3 && v.all { it.isDigit() }) guests = v
                },
                label = { Text("Nombre de personnes") },
                singleLine = true,
                isError = !guestsOk,
                supportingText = if (!guestsOk) {
                    { Text("Entrez un nombre entre 1 et 500.") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Précisions") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = {
                    val message = listOf(
                        "Bonjour Guest Kingakati,",
                        "je souhaite faire une demande de réservation.",
                        "",
                        "Nom : ${name.trim()}",
                        "Téléphone : ${phone.trim()}",
                        "Service : $service",
                        "Date : ${date.trim()}",
                        "Personnes : $guests",
                        "Précisions : ${details.trim()}"
                    ).joinToString("\n")

                    HistoryStore.add(
                        context,
                        "$service • ${date.trim()} • $guests pers. • ${name.trim()}"
                    )
                    openWhatsApp(context, message)
                },
                enabled = formOk,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Dark
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Envoyer la demande")
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Historique local des demandes                                      */
/* ------------------------------------------------------------------ */

@Composable
fun HistoryPage() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(HistoryStore.load(context)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Mes demandes", color = Gold, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Demandes préparées depuis ce téléphone. " +
                    "La confirmation se fait sur WhatsApp.",
                color = Cream
            )
        }

        if (entries.isEmpty()) {
            item { Text("Aucune demande pour le moment.", color = Cream) }
        } else {
            items(entries) { entry ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(entry, color = Cream, modifier = Modifier.padding(16.dp))
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        HistoryStore.clear(context)
                        entries = emptyList()
                    }
                ) {
                    Text("Effacer l'historique")
                }
            }
        }
    }
}

object HistoryStore {
    private const val PREFS = "guest_kingakati"
    private const val KEY = "history"
    private const val SEP = "\u001E"

    fun load(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split(SEP)
    }

    fun add(context: Context, entry: String) {
        val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())
        val clean = "$stamp — $entry".replace(SEP, " ")
        val updated = (listOf(clean) + load(context)).take(50)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, updated.joinToString(SEP))
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }
}

/* ------------------------------------------------------------------ */
/*  Validation                                                         */
/* ------------------------------------------------------------------ */

fun isValidPhone(phone: String): Boolean {
    val digits = phone.count { it.isDigit() }
    return digits in 9..15
}

fun isValidDate(input: String): Boolean {
    val text = input.trim()
    if (!Regex("""\d{1,2}/\d{1,2}/\d{4}""").matches(text)) return false

    val format = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).apply { isLenient = false }
    val parsed = try {
        format.parse(text)
    } catch (e: Exception) {
        null
    } ?: return false

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    return !parsed.before(today)
}

/* ------------------------------------------------------------------ */
/*  WhatsApp                                                           */
/* ------------------------------------------------------------------ */

fun openWhatsApp(context: Context, message: String) {
    // URLEncoder met des "+" pour les espaces : WhatsApp attend %20
    val encoded = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
    val url = "https://wa.me/$WHATSAPP_NUMBER?text=$encoded"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "Impossible d'ouvrir WhatsApp. Installez-le ou utilisez un navigateur.",
            Toast.LENGTH_LONG
        ).show()
    }
}
