package com.aurastudio.data.viewmodel

import android.content.Context
import com.aurastudio.R
import com.aurastudio.data.models.Project
import com.aurastudio.data.project.CreateProjectRequest
import com.aurastudio.data.project.ProjectCreator
import com.aurastudio.data.project.ProjectTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Manages project creation/reporting in the device's Internal Storage
 * (/storage/emulated/0/AuraStudio).
 */
class ProjectsViewModel(private val context: Context) : ViewModel() {

    data class CreateState(
        val isCreating: Boolean = false,
        val error: String? = null,
        val created: Project? = null,
        val needsStorageAccess: Boolean = false
    )

    private val creator = ProjectCreator(context)

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _createState = MutableStateFlow(CreateState())
    val createState: StateFlow<CreateState> = _createState.asStateFlow()

    init {
        refreshProjects()
    }

    fun refreshProjects() {
        viewModelScope.launch {
            _projects.value = withContext(Dispatchers.IO) { creator.scanProjects() }
        }
    }

    fun create(name: String, packageName: String, template: ProjectTemplate) {
        viewModelScope.launch {
            _createState.value = CreateState(isCreating = true)
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    creator.create(
                        CreateProjectRequest(
                            name = name,
                            packageName = packageName,
                            template = template
                        )
                    )
                }
            }
            result.onSuccess { project ->
                _createState.value = CreateState(created = project)
                refreshProjects()
            }.onFailure { e ->
                val message = when {
                    e.message == ProjectCreator.STORAGE_ACCESS_REQUIRED ->
                        context.getString(R.string.create_project_storage_denied)
                    e is IllegalArgumentException && e.message?.isNotBlank() == true ->
                        context.getString(R.string.create_project_exists, e.message)
                    else -> context.getString(R.string.create_project_error)
                }
                val needsAccess = e.message == ProjectCreator.STORAGE_ACCESS_REQUIRED
                _createState.value = CreateState(error = message, needsStorageAccess = needsAccess)
            }
        }
    }

    fun clearCreated() {
        if (_createState.value.created != null) {
            _createState.value = _createState.value.copy(created = null)
        }
    }

    /** Clear a stale error (e.g. storage-access error) — called once access is granted. */
    fun dismissError() {
        val s = _createState.value
        if (s.error != null || s.needsStorageAccess) {
            _createState.value = s.copy(error = null, needsStorageAccess = false)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProjectsViewModel(context.applicationContext)
            }
        }
    }
}