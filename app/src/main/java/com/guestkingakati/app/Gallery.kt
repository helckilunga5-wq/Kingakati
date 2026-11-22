package com.guestkingakati.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

const val PHOTOS_API = "https://api.github.com/repos/helckilunga5-wq/kingakati-photos/contents"
const val PHOTOS_BASE = "https://helckilunga5-wq.github.io/kingakati-photos/"

suspend fun fetchPhotoUrls(): List<String> = withContext(Dispatchers.IO) {
    try {
        val c = URL(PHOTOS_API).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "GuestKingakati")
        val text = c.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val name = arr.getJSONObject(i).getString("name")
            val n = name.lowercase()
            if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp")) {
                if (!n.startsWith("apple-touch-icon")) out.add(PHOTOS_BASE + name)
            }
        }
        out
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun PhotoGallery() {
    var urls by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) { urls = fetchPhotoUrls() }
    if (urls.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Découvrez Kingakati",
                color = Gold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            urls.forEach { u ->
                AsyncImage(
                    model = u,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }
}
