import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URI

plugins {
    id("com.android.library")
}

android {
    namespace = "com.termux"
    compileSdk = 36
    ndkVersion = "27.3.13750724"

    defaultConfig {
        minSdk = 26

        manifestPlaceholders["TERMUX_PACKAGE_NAME"] = "com.aurastudio"
        manifestPlaceholders["TERMUX_APP_NAME"] = "AuraStudio"

        externalNativeBuild {
            cmake {
                abiFilters += setOf("arm64-v8a")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    api(project(":termux:shared"))
    api(project(":termux:view"))

    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.viewpager:viewpager:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.guava:guava:33.4.0-android")

    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")
    implementation("io.noties.markwon:recycler:4.6.2")
}

// ---- Custom bootstrap (com.aurastudio) integration ---------------------------
// Downloaded from the newest bootstrap-* release of Arata-Labs/aurastudio-termux,
// verified against its SHA256SUMS.txt and embedded into libtermux-bootstrap.so via
// termux-bootstrap-zip.S (same approach as the official Termux app build).

val BOOTSTRAP_REPO = "Arata-Labs/aurastudio-termux"

fun githubApi(path: String): String {
    val conn = URI("https://api.github.com/$path").toURL().openConnection() as HttpURLConnection
    conn.setRequestProperty("Accept", "application/vnd.github+json")
    conn.setRequestProperty("User-Agent", "AuraStudioBuild")
    conn.instanceFollowRedirects = true
    conn.connect()
    if (conn.responseCode != 200) {
        throw GradleException("GitHub API $path: HTTP ${conn.responseCode}")
    }
    return conn.inputStream.bufferedReader().use { it.readText() }
}

fun fetchText(url: String): String {
    val conn = URI(url).toURL().openConnection() as HttpURLConnection
    conn.instanceFollowRedirects = true
    conn.connect()
    if (conn.responseCode != 200) {
        throw GradleException("Failed to fetch $url: HTTP ${conn.responseCode}")
    }
    return conn.inputStream.bufferedReader().use { it.readText() }
}

data class BootstrapRelease(val tag: String, val assets: Map<String, String>)

fun latestBootstrapRelease(): BootstrapRelease {
    val tagsBody = githubApi("repos/$BOOTSTRAP_REPO/releases?per_page=30")
    val tag = Regex("\"tag_name\":\\s*\"([^\"]+)\"")
        .findAll(tagsBody).map { it.groupValues[1] }
        .firstOrNull { it.startsWith("bootstrap-") }
        ?: throw GradleException("No bootstrap-* release found in $BOOTSTRAP_REPO")

    val body = githubApi("repos/$BOOTSTRAP_REPO/releases/tags/$tag")
    val assets = Regex("\"name\":\\s*\"((?:bootstrap-|SHA256SUMS\\.txt)[^\"]*)\"[\\s\\S]*?\"browser_download_url\":\\s*\"([^\"]+)\"")
        .findAll(body)
        .associate { it.groupValues[1] to it.groupValues[2] }

    logger.quiet("Bootstrap release: $tag (${assets.size} asset(s))")
    return BootstrapRelease(tag, assets)
}

fun searchFile(dir: File, name: String): String {
    val f = File(dir, name)
    return if (f.exists()) f.absolutePath else name
}

fun sha256File(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { stream ->
        val buffer = ByteArray(8192)
        var read: Int
        while (stream.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { b -> String.format("%02x", b) }
}

fun downloadBootstrap(arch: String, release: BootstrapRelease) {
    val dir = file("src/main/cpp")
    val outFile = File(dir, "bootstrap-$arch.zip")

    val remoteUrl = release.assets["bootstrap-$arch.zip"]
        ?: release.assets["bootstrap-bootstrap-$arch.zip"]
        ?: throw GradleException("No bootstrap-$arch.zip asset in release ${release.tag}")

    var expectedSha: String? = null
    release.assets["SHA256SUMS.txt"]?.let { sumsUrl ->
        val assetName = remoteUrl.substringAfterLast('/')
        expectedSha = fetchText(sumsUrl).lineSequence()
            .firstOrNull { it.trim().endsWith(" $assetName") }
            ?.trim()?.split(Regex("\\s+"))?.first()
        if (expectedSha == null) {
            logger.warn("SHA256SUMS.txt has no entry for $assetName; skipping checksum")
        }
    }

    if (outFile.exists()) {
        if (expectedSha != null && sha256File(outFile) == expectedSha) {
            logger.quiet("Bootstrap $arch ($release.tag): checksum OK, skipping download")
            return
        }
        logger.quiet("Bootstrap $arch: checksum mismatch, re-downloading")
        outFile.delete()
    }

    dir.mkdirs()
    logger.quiet("Downloading $remoteUrl ...")
    val conn = URI(remoteUrl).toURL().openConnection() as HttpURLConnection
    conn.instanceFollowRedirects = true
    conn.connect()
    if (conn.responseCode != 200) {
        throw GradleException("Failed to download bootstrap: HTTP ${conn.responseCode} from $remoteUrl")
    }
    outFile.outputStream().buffered().use { out ->
        conn.inputStream.buffered().use { input -> input.copyTo(out) }
    }
    conn.disconnect()

    if (expectedSha != null) {
        val hash = sha256File(outFile)
        if (hash != expectedSha) {
            outFile.delete()
            throw GradleException("Wrong checksum for bootstrap-$arch.zip: expected $expectedSha, actual $hash")
        }
    }
}

tasks.register("downloadBootstraps") {
    doLast {
        val release = latestBootstrapRelease()
        downloadBootstrap("aarch64", release)
    }
}

tasks.named("preBuild") {
    dependsOn("downloadBootstraps")
}