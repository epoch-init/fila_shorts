package zeki.productions.shorts.ui.screens

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * Sterilized Settings Interface.
 * V1.7.0: Integrated About routing and separated operations visually.
 */
@Composable
fun SettingsScreen(
    videoCount: Int,
    onDeleteSeen: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val shortsDir = File(Environment.getExternalStorageDirectory(), "Shorts")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(40.dp))

        SettingNode("Theme", "Blood Red / Oxblood")
        SettingNode("Root Path", shortsDir.absolutePath)
        SettingNode("Current Videos", "$videoCount records indexed")

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNavigateToAbout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A0000),
                contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text("ABOUT THIS APP", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onDeleteSeen,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B0000),
                contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text("DELETE ALL SEEN SHORTS", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingNode(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF8B0000),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}