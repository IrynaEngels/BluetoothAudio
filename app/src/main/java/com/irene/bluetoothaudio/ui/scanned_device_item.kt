package com.irene.bluetoothaudio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScannedDeviceItem(name: String, address: String, connect: (macAddress: String) -> Unit){
    Column(Modifier.padding(16.dp).clickable { connect(address) },
        verticalArrangement = Arrangement.Center) {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
        Text(address, fontSize = 14.sp)
        Divider()
    }
}