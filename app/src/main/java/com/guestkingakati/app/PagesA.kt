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

@Composable
fun HomePage(onNavigate: (page: String, service: String?) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("VOTRE DESTINATION DE DÉTENTE", color = Gold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Bienvenue chez\nGuest Kingakati",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Cream
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Un lieu pour séjourner, savourer, célébrer et se ressourcer.",
                        color = Cream
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { onNavigate("Réserver", null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = Dark
                        )
                    ) {
                        Text("Réserver maintenant")
                    }
                }
            }
        }
item { PhotoGallery() }
        item {
            ServiceCard(
                title = "Hôtel",
                description = "Découvrez nos chambres climatisées avec petit déjeuner.",
                onClick = { onNavigate("Hôtel", null) }
            )
        }

        item {
            ServiceCard(
                title = "Restaurant",
                description = "Cuisine chinoise, européenne, africaine et italienne.",
                onClick = { onNavigate("Restaurant", null) }
            )
        }

        item {
            ServiceCard(
                title = "Événements",
                description = "Mariages, anniversaires et autres manifestations : " +
                    "salle VIP et jardin pour vos célébrations et réunions.",
                buttonLabel = "Faire une demande",
                onClick = { onNavigate("Réserver", "Mariage") }
            )
        }

        item {
            ServiceCard(
                title = "Loisirs",
                description = "Jeux pour enfants, chaise de massage et plusieurs autres activités de détente.",
                buttonLabel = "Faire une demande",
                onClick = { onNavigate("Réserver", "Loisirs") }
            )
        }
    }
}

@Composable
fun ServiceCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    buttonLabel: String = "Découvrir",
    price: String? = null,
    imageUrl: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                )
            }
            Column(Modifier.padding(20.dp)) {
                Text(title, color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(description, color = Cream)
                }
                if (price != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(price, color = Gold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onClick) {
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
fun RefreshButton(loading: Boolean, onRefresh: () -> Unit) {
    if (!ONLINE_ENABLED) return
    OutlinedButton(onClick = onRefresh, enabled = !loading) {
        Text(if (loading) "Mise à jour…" else "Actualiser les tarifs")
    }
}

/* ------------------------------------------------------------------ */
/*  Hôtel                                                              */
/* ------------------------------------------------------------------ */

@Composable
fun HotelPage(
    rooms: List<CatalogItem>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onReserve: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Hôtel & chambres", color = Gold, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text("Disponibilités à confirmer auprès de l'établissement.", color = Cream)
            Spacer(Modifier.height(4.dp))
            Text(
                "Une salle d'attente est à la disposition des visiteurs de nos clients.",
                color = Cream
            )
            Spacer(Modifier.height(8.dp))
            RefreshButton(loading, onRefresh)
        }

        if (rooms.isEmpty()) {
            item { Text("Aucune chambre à afficher pour le moment.", color = Cream) }
        }

        items(rooms) { room ->
            ServiceCard(
                title = room.name,
                description = room.description,
                price = formatPrice(room.priceUsd, room.priceFc) + " la chambre",
                imageUrl = room.photoUrl,
                buttonLabel = "Réserver",
                onClick = onReserve
            )
        }

        item {
            Button(
                onClick = onReserve,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Dark
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Demander une réservation")
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Restaurant                                                         */
/* ------------------------------------------------------------------ */

@Composable
fun MenuRow(item: CatalogItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (item.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = item.photoUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, color = Cream, fontWeight = FontWeight.Bold)
                    if (item.description.isNotBlank()) {
                        Text(item.description, color = Cream, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(formatPrice(item.priceUsd, item.priceFc), color = Gold)
            }
        }
    }
}

@Composable
fun RestaurantPage(
    dishes: List<CatalogItem>,
    drinks: List<CatalogItem>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onReserve: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Restaurant", color = Gold, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Cuisine chinoise, européenne, africaine et italienne.",
                color = Cream
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Les prix peuvent changer : confirmez auprès de l'établissement.",
                color = Cream
            )
            Spacer(Modifier.height(8.dp))
            RefreshButton(loading, onRefresh)
        }

        if (dishes.isNotEmpty()) {
            item { Text("Plats", color = Gold, fontSize = 22.sp) }
            items(dishes) { MenuRow(it) }
        }

        if (drinks.isNotEmpty()) {
            item { Text("Boissons", color = Gold, fontSize = 22.sp) }
            items(drinks) { MenuRow(it) }
        }

        if (dishes.isEmpty() && drinks.isEmpty()) {
            item { Text("Le menu sera bientôt disponible ici.", color = Cream) }
        }

        item {
            Button(
                onClick = {
                    openWhatsApp(
                        context,
                        "Bonjour Guest Kingakati, je voudrais consulter le menu."
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Dark
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Demander le menu sur WhatsApp")
            }
        }

        item {
            OutlinedButton(
                onClick = onReserve,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Réserver une table")
            }
        }
    }
}
