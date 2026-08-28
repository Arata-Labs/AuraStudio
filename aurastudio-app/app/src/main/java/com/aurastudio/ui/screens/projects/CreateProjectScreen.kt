package com.aurastudio.ui.screens.projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aurastudio.R
import com.aurastudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    scaffoldPadding: PaddingValues = PaddingValues(),
    onCreateProject: (name: String, type: String, path: String) -> Unit = { _, _, _ -> }
) {
    var projectName by remember { mutableStateOf("") }
    var selectedType by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }

    val projectTypes = listOf(
        stringResource(R.string.create_project_type_gradle_java),
        stringResource(R.string.create_project_type_gradle_kotlin),
        stringResource(R.string.create_project_type_native_cpp),
        stringResource(R.string.create_project_type_ndk)
    )

    val projectTypeKeys = listOf("gradle_java", "gradle_kotlin", "native_cpp", "ndk")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = scaffoldPadding.calculateTopPadding() + 8.dp,
                bottom = scaffoldPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.create_project_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Project Name
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardBg()
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_project_name_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.create_project_name_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }

        // Project Type
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardBg()
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_project_type_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = projectTypes[selectedType],
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        projectTypes.forEachIndexed { index, type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = index
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        when (index) {
                                            0 -> Icons.Filled.Code
                                            1 -> Icons.Filled.Code
                                            2 -> Icons.Filled.Memory
                                            else -> Icons.Filled.Layers
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Create Button
        Button(
            onClick = {
                if (projectName.isNotBlank()) {
                    onCreateProject(projectName, projectTypeKeys[selectedType], "")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = projectName.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.create_project_btn),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
