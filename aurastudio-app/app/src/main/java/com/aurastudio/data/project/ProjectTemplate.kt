package com.aurastudio.data.project

import androidx.annotation.StringRes
import com.aurastudio.R
import com.aurastudio.data.models.ProjectType

data class CreateProjectRequest(
    val name: String,
    val packageName: String = "",
    val template: ProjectTemplate
)

/**
 * The project templates offered by the Create Project screen.
 * [key] identifies the template (also written to a marker file inside the
 * generated project so the Projects screen can recover its type).
 */
enum class ProjectTemplate(
    val key: String,
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int,
    val projectType: ProjectType
) {
    GRADLE_KOTLIN(
        key = "gradle_kotlin",
        nameRes = R.string.template_app_kotlin,
        descRes = R.string.template_app_kotlin_desc,
        projectType = ProjectType.GRADLE_KOTLIN
    ),
    GRADLE_JAVA(
        key = "gradle_java",
        nameRes = R.string.template_app_java,
        descRes = R.string.template_app_java_desc,
        projectType = ProjectType.GRADLE_JAVA
    ),
    MATERIAL_YOU(
        key = "material_you",
        nameRes = R.string.template_app_material_you,
        descRes = R.string.template_app_material_you_desc,
        projectType = ProjectType.MATERIAL_YOU
    ),
    COMPOSE(
        key = "compose",
        nameRes = R.string.template_app_compose,
        descRes = R.string.template_app_compose_desc,
        projectType = ProjectType.COMPOSE
    ),
    ANDROID_LIBRARY(
        key = "android_library",
        nameRes = R.string.template_android_library,
        descRes = R.string.template_android_library_desc,
        projectType = ProjectType.ANDROID_LIBRARY
    ),
    JAVA_LIBRARY(
        key = "java_library",
        nameRes = R.string.template_java_library,
        descRes = R.string.template_java_library_desc,
        projectType = ProjectType.JAVA_LIBRARY
    ),
    KOTLIN_LIBRARY(
        key = "kotlin_library",
        nameRes = R.string.template_kotlin_library,
        descRes = R.string.template_kotlin_library_desc,
        projectType = ProjectType.KOTLIN_LIBRARY
    ),
    NATIVE_CPP(
        key = "native_cpp",
        nameRes = R.string.template_native_cpp,
        descRes = R.string.template_native_cpp_desc,
        projectType = ProjectType.NATIVE_CPP
    ),
    NDK(
        key = "ndk",
        nameRes = R.string.template_ndk,
        descRes = R.string.template_ndk_desc,
        projectType = ProjectType.NDK_SHARED_LIB
    );

    companion object {
        fun fromKey(key: String): ProjectTemplate? = entries.firstOrNull { it.key == key }
    }
}