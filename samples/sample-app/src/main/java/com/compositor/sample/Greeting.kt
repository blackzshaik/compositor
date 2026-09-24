package com.compositor.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF232323))
            .padding(24.dp)
    ) {
        Text(
            text = "Welcome to the Future of Android Compose Design, $name!",
            color = Color.White,
            fontSize = 20.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Greeting("Blackz")
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    
    Greeting(" It takes some time to load")
}
