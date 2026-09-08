package com.example.android_manager.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.permissions.AppPermissionInfo
import com.example.android_manager.data.permissions.PermissionRepository
import com.example.android_manager.ui.components.APVMBottomBar
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PermissionAuditorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {

    val context = LocalContext.current

    val appPermissions = produceState(
        initialValue = emptyList<AppPermissionInfo>(),
        key1 = context
    ) {

        value = withContext(Dispatchers.IO) {

            val apps =
                AppRepository(context)
                    .getInstalledApps()

            PermissionRepository(context)
                .getAppPermissions(apps)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {

        APVMTopBar()

        Text(
            text = "← BACK",
            color = SecondaryText,
            modifier = Modifier
                .padding(
                    horizontal = 20.dp,
                    vertical = 8.dp
                )
                .clickable {
                    onBack()
                }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "PERMISSION AUDITOR",
                    color = PrimaryText
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "${appPermissions.value.size} APPLICATIONS AUDITED",
                    color = SecondaryText
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            items(
                appPermissions.value
            ) { app ->

                PermissionAppCard(
                    app = app
                )
            }

            item {

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }

        APVMBottomBar()
    }
}

@Composable
private fun PermissionAppCard(
    app: AppPermissionInfo
) {

    val dangerous =
        app.dangerousPermissions.size

    val special =
        app.specialPermissions.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Color(0xFF111111)
            )
            .padding(16.dp)
    ) {

        Text(
            text = app.app.name,
            color = PrimaryText
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = app.app.packageName,
            color = SecondaryText
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "DANGEROUS: $dangerous",
                color = if (dangerous > 0)
                    PrimaryText
                else
                    SecondaryText
            )

            Text(
                text = "SPECIAL: $special",
                color = if (special > 0)
                    PrimaryText
                else
                    SecondaryText
            )
        }

        if (dangerous > 0) {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            app.dangerousPermissions
                .take(4)
                .forEach { permission ->

                    Text(
                        text = permission.name,
                        color = SecondaryText,
                        modifier = Modifier.padding(
                            top = 4.dp
                        )
                    )
                }
        }

        if (special > 0) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            app.specialPermissions
                .take(3)
                .forEach { permission ->

                    Text(
                        text = permission.name,
                        color = SecondaryText,
                        modifier = Modifier.padding(
                            top = 4.dp
                        )
                    )
                }
        }
    }
}