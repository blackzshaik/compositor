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

@Preview(showBackground = true)
@Composable
fun ResourceGreetingPreview() {
    Greeting(androidx.compose.ui.res.stringResource(R.string.sample_resource_string))
}

@Preview(showBackground = true)
@Composable
fun IconGreetingPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF232323))
            .padding(24.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_sample_dot),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.Unspecified
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.sample_resource_string),
                color = Color.White,
                fontSize = 20.sp
            )
        }
    }
}
