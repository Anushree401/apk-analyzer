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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.permissions.AppPermissionInfo
import com.example.android_manager.data.permissions.PermissionRepository
import com.example.android_manager.ui.components.APVMBottomBar
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Graphite
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PermissionAuditorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val appRepository = remember {
        AppRepository(context)
    }

    val permissionRepository = remember {
        PermissionRepository(context)
    }

    var auditedApps by remember {
        mutableStateOf<List<AppPermissionInfo>?>(null)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var isAuditing by remember {
        mutableStateOf(false)
    }

    var auditError by remember {
        mutableStateOf<String?>(null)
    }

    suspend fun performAudit() {

        isAuditing = true
        auditError = null

        try {

            val result =
                withContext(Dispatchers.IO) {

                    val apps =
                        appRepository.getInstalledApps()

                    permissionRepository
                        .getAppPermissions(apps)
                }

            auditedApps = result

        } catch (e: Exception) {

            auditError =
                e.message
                    ?: "Permission audit failed."

        } finally {

            isAuditing = false
        }
    }

    LaunchedEffect(Unit) {
        performAudit()
    }

    DisposableEffect(lifecycleOwner) {

        val observer =
            LifecycleEventObserver { _, event ->

                if (
                    event ==
                    Lifecycle.Event.ON_RESUME
                ) {

                    if (
                        auditedApps != null
                    ) {
                        isAuditing = true
                    }
                }
            }

        lifecycleOwner.lifecycle
            .addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle
                .removeObserver(observer)
        }
    }

    val filteredApps =
        auditedApps?.filter { appInfo ->

            appInfo.app.name.contains(
                searchQuery,
                ignoreCase = true
            ) ||

                    appInfo.app.packageName.contains(
                        searchQuery,
                        ignoreCase = true
                    )

        } ?: emptyList()

    val dangerousCount =
        auditedApps?.sumOf {
            it.dangerousPermissions.size
        } ?: 0

    val specialCount =
        auditedApps?.sumOf {
            it.specialPermissions.size
        } ?: 0


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {

        APVMTopBar(
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp
                )
        ) {

            Text(
                text = "PERMISSION AUDITOR",
                color = PrimaryText,
                fontSize = 22.sp,
                fontWeight =
                    FontWeight.Medium
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    when {

                        isAuditing ->
                            "AUDITING APPLICATIONS..."

                        auditedApps == null ->
                            "PREPARING AUDIT..."

                        else ->
                            "${auditedApps!!.size} APPLICATIONS AUDITED"
                    },

                color = SecondaryText,
                fontSize = 14.sp
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            if (isAuditing) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Surface,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(28.dp),
                        color = Graphite
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            "AUDITING INSTALLED APPLICATIONS",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight =
                            FontWeight.Medium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Checking requested permissions and security classifications...",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )
            }

            auditError?.let { error ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Surface,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {

                    Text(
                        text = "AUDIT FAILED",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight =
                            FontWeight.Medium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text = error,
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )
            }

            if (
                auditedApps != null &&
                !isAuditing
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    PermissionStatCard(
                        value =
                            auditedApps!!
                                .size
                                .toString(),

                        label =
                            "APPLICATIONS",

                        modifier =
                            Modifier.weight(1f)
                    )

                    PermissionStatCard(
                        value =
                            dangerousCount
                                .toString(),

                        label =
                            "DANGEROUS",

                        modifier =
                            Modifier.weight(1f)
                    )

                    PermissionStatCard(
                        value =
                            specialCount
                                .toString(),

                        label =
                            "SPECIAL",

                        modifier =
                            Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    BasicTextField(
                        value = searchQuery,

                        onValueChange = {
                            searchQuery = it
                        },

                        modifier = Modifier
                            .weight(1f)
                            .background(
                                Surface,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(
                                horizontal = 14.dp,
                                vertical = 13.dp
                            ),

                        singleLine = true,

                        textStyle =
                            TextStyle(
                                color = PrimaryText,
                                fontSize = 14.sp
                            ),

                        cursorBrush =
                            SolidColor(
                                PrimaryText
                            ),

                        decorationBox = {
                                innerTextField ->

                            if (
                                searchQuery.isEmpty()
                            ) {

                                Text(
                                    text =
                                        "Search applications...",
                                    color =
                                        SecondaryText,
                                    fontSize = 14.sp
                                )
                            }

                            innerTextField()
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.padding(
                                horizontal = 5.dp
                            )
                    )

                    Text(
                        text = "AUDIT",
                        color = PrimaryText,
                        modifier = Modifier
                            .background(
                                Graphite,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(
                                horizontal = 14.dp,
                                vertical = 13.dp
                            )
                            .then(
                                Modifier
                            ),
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Medium
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )
            }
        }

        if (
            auditedApps != null &&
            !isAuditing
        ) {

            if (
                filteredApps.isEmpty()
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            "NO APPLICATIONS FOUND",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight =
                            FontWeight.Medium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "No applications match your search.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 20.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items = filteredApps,
                        key = {
                            it.app.packageName
                        }
                    ) { appInfo ->

                        PermissionAppCard(
                            appInfo = appInfo
                        )
                    }

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(80.dp)
                        )
                    }
                }
            }
        }
    }

    APVMBottomBar()
}

@Composable
private fun PermissionStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .background(
                Surface,
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {

        Text(
            text = value,
            color = PrimaryText,
            fontSize = 20.sp,
            fontWeight =
                FontWeight.Medium
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text = label,
            color = SecondaryText,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PermissionAppCard(
    appInfo: AppPermissionInfo
) {

    val risk =
        when {

            appInfo.specialPermissions.isNotEmpty() ->
                "SPECIAL"

            appInfo.dangerousPermissions.isNotEmpty() ->
                "DANGEROUS"

            else ->
                "NORMAL"
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Surface,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        appInfo.app.name,
                    color = PrimaryText,
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.Medium
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        appInfo.app.packageName,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }

            Text(
                text = risk,
                color = PrimaryText,
                fontSize = 11.sp,
                fontWeight =
                    FontWeight.Medium
            )
        }


        if (
            appInfo.dangerousPermissions
                .isNotEmpty()
        ) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    "DANGEROUS PERMISSIONS",
                color = SecondaryText,
                fontSize = 10.sp,
                fontWeight =
                    FontWeight.Medium
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            appInfo.dangerousPermissions
                .take(5)
                .forEach { permission ->

                    Text(
                        text =
                            permission.name,
                        color = PrimaryText,
                        fontSize = 12.sp,
                        modifier =
                            Modifier.padding(
                                vertical = 2.dp
                            )
                    )
                }
        }


        if (
            appInfo.specialPermissions
                .isNotEmpty()
        ) {

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text =
                    "SPECIAL PERMISSIONS",
                color = SecondaryText,
                fontSize = 10.sp,
                fontWeight =
                    FontWeight.Medium
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            appInfo.specialPermissions
                .take(5)
                .forEach { permission ->

                    Text(
                        text =
                            permission.name,
                        color = PrimaryText,
                        fontSize = 12.sp,
                        modifier =
                            Modifier.padding(
                                vertical = 2.dp
                            )
                    )
                }
        }
    }
}