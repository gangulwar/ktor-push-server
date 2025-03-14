package com.gangulwar.ktor_push

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gangulwar.ktor_push.service.NotificationService
import com.gangulwar.ktor_push.service.ServiceManager

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(serviceContext: Context) {
    val notificationService = remember { NotificationService() }
    val serviceState by ServiceManager.serviceState.collectAsState()
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        ServiceManager.updateServiceStatus(serviceContext)
    }

    val primaryColor = Color(0xFF3F51B5)
    val accentColor = Color(0xFF4CAF50)
    val backgroundColor = Color(0xFFF5F5F5)
    val surfaceColor = Color(0xFFFFFFFF)
    val errorColor = Color(0xFFE53935)
    val warningColor = Color(0xFFFFA000)
    val textPrimaryColor = Color(0xFF212121)
    val textSecondaryColor = Color(0xFF757575)

    val (statusColor, statusText, statusDescription, statusIcon) = when (serviceState) {
        is ServiceManager.ServiceState.Running -> Quadruple(
            accentColor,
            "ACTIVE",
            "Your notifications are being monitored",
            Icons.Rounded.Notifications
        )

        is ServiceManager.ServiceState.Connecting -> Quadruple(
            warningColor,
            "CONNECTING",
            "Attempt ${(serviceState as ServiceManager.ServiceState.Connecting).attempt}...",
            Icons.Rounded.Refresh
        )

        is ServiceManager.ServiceState.Error -> {
            val errorState = serviceState as ServiceManager.ServiceState.Error
            Quadruple(
                errorColor,
                "ERROR",
                "${errorState.message}. Reconnecting in ${errorState.reconnectIn}s",
                Icons.Rounded.Warning
            )
        }

        else -> Quadruple(
            errorColor,
            "INACTIVE",
            "Start the service to enable notifications",
            Icons.Rounded.Close
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notification Panel",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )

                AssistChip(
                    onClick = {

                    },
                    label = {
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor
                    ),
                    border = null
                )

            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = surfaceColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = statusColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "Status",
                            tint = statusColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (serviceState) {
                            is ServiceManager.ServiceState.Running -> "Service Running"
                            is ServiceManager.ServiceState.Connecting -> "Connecting to Service"
                            is ServiceManager.ServiceState.Error -> "Service Error"
                            else -> "Service Stopped"
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimaryColor
                    )

                    Text(
                        text = statusDescription,
                        fontSize = 14.sp,
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = surfaceColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Notification Message",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimaryColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = {
                            Text(
                                "Type your message here...",
                                color = textSecondaryColor
                            )
                        },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = textSecondaryColor,
                            cursorColor = primaryColor,
                            focusedTextColor = textPrimaryColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { notificationService.sendNotification(message) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        enabled = serviceState is ServiceManager.ServiceState.Running && message.isNotEmpty()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Send,
                                contentDescription = "Send",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SEND NOTIFICATION",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = surfaceColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Service Controls",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimaryColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { ServiceManager.startService(serviceContext) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            enabled = serviceState is ServiceManager.ServiceState.Stopped
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Start",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "START",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = { ServiceManager.stopService(serviceContext) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = errorColor),
                            shape = RoundedCornerShape(12.dp),
                            enabled = serviceState !is ServiceManager.ServiceState.Stopped
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Stop",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "STOP",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
