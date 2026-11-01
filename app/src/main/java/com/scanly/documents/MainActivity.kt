package com.scanly.documents

import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScanDoc(val title: String, val date: String, val pages: Int)

class MainActivity : ComponentActivity() {
    private val documents = mutableStateListOf<ScanDoc>()
    private val options by lazy {
        GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(50)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF, GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ScanlyHome(documents, { scan() }, { picker.launch(arrayOf("image/*", "application/pdf")) }) }
    }
    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.filter { contentResolver.getType(it) == "application/pdf" }.forEach {
            savePdf(it, "Imported_${System.currentTimeMillis()}.pdf")
            documents.add(0, ScanDoc("מסמך מיובא", today(), 1))
        }
    }
    private fun scan() {
        GmsDocumentScanning.getClient(options).getStartScanIntent(this).addOnSuccessListener {
            scanner.launch(IntentSenderRequest.Builder(it).build())
        }
    }
    private val scanner = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pdf?.let { pdf ->
                savePdf(pdf.uri, "Scanly_${System.currentTimeMillis()}.pdf")
                documents.add(0, ScanDoc("סריקה חדשה", today(), pdf.pageCount))
            }
        }
    }
    private fun savePdf(source: android.net.Uri, name: String) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val dest = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        try {
            contentResolver.openInputStream(source)?.use { input -> contentResolver.openOutputStream(dest)?.use(input::copyTo) }
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(dest, values, null, null)
        } catch (_: Exception) { contentResolver.delete(dest, null, null) }
    }
    private fun today() = SimpleDateFormat("dd.MM.yyyy", Locale("he", "IL")).format(Date())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanlyHome(docs: List<ScanDoc>, onScan: () -> Unit, onImport: () -> Unit) {
    var search by remember { mutableStateOf("") }
    val purple = Color(0xFF6558D3)
    val filtered = docs.filter { it.title.contains(search, true) }
    MaterialTheme(colorScheme = lightColorScheme(primary = purple, background = Color(0xFFF6F7FB))) {
        Scaffold(
            containerColor = Color(0xFFF6F7FB),
            floatingActionButton = { ExtendedFloatingActionButton(onClick = onScan, containerColor = purple, contentColor = Color.White, icon = { Icon(Icons.Default.DocumentScanner, null) }, text = { Text("סריקת מסמך") }) },
            bottomBar = { NavigationBar(containerColor = Color.White) {
                NavigationBarItem(true, {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("בית") })
                NavigationBarItem(false, onImport, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("תיקיות") })
                NavigationBarItem(false, {}, icon = { Icon(Icons.Default.Search, null) }, label = { Text("חיפוש") })
                NavigationBarItem(false, {}, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("הגדרות") })
            } }
        ) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Spacer(Modifier.height(18.dp))
                    Text("שלום! 👋", color = Color(0xFF78839A))
                    Text("המסמכים שלך,", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF17243A))
                    Text("מסודרים במקום אחד.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF17243A))
                }
                item {
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), placeholder = { Text("חיפוש מסמך") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(18.dp), singleLine = true)
                }
                item {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF252B4A))) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("הופכים נייר לדיגיטלי", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("סריקה חכמה, חיתוך אוטומטי ויצירת PDF.", color = Color(0xFFD9DDF0))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = onScan) { Icon(Icons.Default.CameraAlt, null); Text(" סריקה") }
                                OutlinedButton(onClick = onImport) { Icon(Icons.Default.FileUpload, null); Text(" ייבוא") }
                            }
                        }
                    }
                }
                item { Text("המסמכים האחרונים · ${docs.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                if (filtered.isEmpty()) item { Text("עדיין אין מסמכים. התחילי בסריקה או בייבוא.", color = Color.Gray) }
                items(filtered) { doc ->
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = purple, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) { Text(doc.title, fontWeight = FontWeight.SemiBold); Text(doc.date, color = Color.Gray) }
                            Text("${doc.pages} עמ׳", color = purple)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
