package com.spiritwisestudios.crossroadsoffate.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.util.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.random.Random

/**
 * Screen demonstrating error logging capabilities.
 * Allows generating different types of errors and viewing logs.
 */
@Composable
fun ErrorLoggerScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var logContent by remember { mutableStateOf<String?>(null) }
    
    // Load logs when screen is first composed
    LaunchedEffect(Unit) {
        logContent = withContext(Dispatchers.IO) {
            ErrorLogger.getErrorLog(context)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Error Logger Demo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Error generation buttons
        Button(
            onClick = {
                // Log a simple error
                val errorMessage = "User initiated simple error log test"
                ErrorLogger.logError(errorMessage)
                ErrorLogger.saveErrorToFile(context, errorMessage)
                Toast.makeText(context, "Error logged", Toast.LENGTH_SHORT).show()
                
                // Reload logs after adding a new entry
                coroutineScope.launch {
                    logContent = withContext(Dispatchers.IO) {
                        ErrorLogger.getErrorLog(context)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log Simple Error")
        }
        
        // ArithmeticException button
        Button(
            onClick = {
                // Perform the calculation outside of the composable lambda
                val divideByZeroResult = {
                    try {
                        1 / (Random.nextInt(2) - 1)
                        null // No exception
                    } catch (e: ArithmeticException) {
                        e // Return the exception
                    }
                }()

                // Handle the exception, if any
                divideByZeroResult?.let { e ->
                    val errorMessage = "Division by zero error occurred"
                    ErrorLogger.logException(e, errorMessage)
                    ErrorLogger.saveErrorToFile(context, errorMessage, e)
                    Toast.makeText(context, "Exception logged", Toast.LENGTH_SHORT).show()
                    
                    // Reload logs after adding a new entry
                    coroutineScope.launch {
                        logContent = withContext(Dispatchers.IO) {
                            ErrorLogger.getErrorLog(context)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simulate ArithmeticException")
        }
        
        // IOException button
        Button(
            onClick = {
                // Create an IOException outside of the composable lambda
                val exception = IOException("Simulated file not found exception")
                val errorMessage = "File operation failed"
                ErrorLogger.logException(exception, errorMessage)
                ErrorLogger.saveErrorToFile(context, errorMessage, exception)
                Toast.makeText(context, "IO Exception logged", Toast.LENGTH_SHORT).show()
                
                // Reload logs after adding a new entry
                coroutineScope.launch {
                    logContent = withContext(Dispatchers.IO) {
                        ErrorLogger.getErrorLog(context)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simulate IOException")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        
        // Log display area
        Text(
            text = "Error Log",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = logContent ?: "No logs found",
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Control buttons at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            ErrorLogger.clearErrorLog(context)
                        }
                        logContent = null
                    }
                    Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Clear Logs")
            }
            
            OutlinedButton(onClick = onBackClick) {
                Text("Back")
            }
        }
    }
}

@Composable
fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment
    ) {
        content()
    }
} 