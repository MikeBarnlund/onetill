package com.onetill.android.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.R
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.WifiPasscodeDialog
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradientBackground

@Composable
fun NoConnectionScreen() {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    var showPasscodeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenGradientBackground(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.onetill_logo),
                contentDescription = "OneTill logo",
                modifier = Modifier.size(120.dp),
            )

            Spacer(modifier = Modifier.height(dimens.xl))

            Text(
                text = "Connect to the Internet",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(dimens.sm))

            Text(
                text = "Setting up your OneTill device requires Wi-Fi or cellular " +
                    "to pair with your store.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(dimens.sm))

            Text(
                text = "Once paired, OneTill works fully offline — including card payments.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(dimens.xl))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = colors.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(dimens.sm))
                Text(
                    text = "Looking for connection…",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dimens.lg, end = dimens.lg, top = 10.dp, bottom = 14.dp),
        ) {
            OneTillButton(
                text = "Open Wi-Fi Settings",
                onClick = { showPasscodeDialog = true },
            )
        }
    }

    if (showPasscodeDialog) {
        WifiPasscodeDialog(onDismiss = { showPasscodeDialog = false })
    }
}
