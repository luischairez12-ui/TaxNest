package com.taxnest.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Original gradient mountain artwork drawn at runtime; concept #1 inspired. */
@Composable
fun MountainHeader(greeting:String) {
    Box(Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(25.dp)).background(
        Brush.linearGradient(listOf(Color(0xFF79B2ED), Color(0xFF8D94E7),Color(0xFFB196ED)))
    )) {
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width;val h=size.height
            drawCircle(color=Color.White.copy(alpha=.20f),radius=h*.33f,center=Offset(w*.78f,h*.33f))
            val back=Path().apply {moveTo(0f,h*.83f);lineTo(w*.19f,h*.55f);lineTo(w*.29f,h*.69f);lineTo(w*.47f,h*.42f);lineTo(w*.66f,h*.74f);lineTo(w*.83f,h*.46f);lineTo(w,h*.72f);lineTo(w,h);lineTo(0f,h);close()}
            drawPath(back,Color(0xFFDAE9FF).copy(alpha=.44f))
            val front=Path().apply{moveTo(0f,h*.95f);lineTo(w*.26f,h*.78f);lineTo(w*.43f,h*.93f);lineTo(w*.70f,h*.62f);lineTo(w,h*.95f);lineTo(w,h);lineTo(0f,h);close()}
            drawPath(front,Color(0xFFCED7FE).copy(alpha=.55f))
        }
        Column(Modifier.padding(19.dp)) {
            Text(greeting,fontSize=22.sp,fontWeight=FontWeight.Bold,color=Color.White)
            Spacer(Modifier.height(5.dp))
            Text("Your money, your records, your taxes —\nall in one beautiful place.",fontSize=12.sp,color=Color.White.copy(alpha=.95f),lineHeight=18.sp)
        }
    }
}
