package io.lanzof.ingestor.gtfs.archive

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.util.Properties

@Component
class BkkStaticGtfsArchiveProvider(
    @param:Value("\${budatravel.gtfs.bkk-static.url}") private val url: String,
    @param:Value("\${budatravel.gtfs.bkk-static.cache-dir}") cacheDir: String,
) {
    private val cacheDir: Path = Path.of(cacheDir)
    private val archivePath: Path = this.cacheDir.resolve(ARCHIVE_FILE_NAME)
    private val metadataPath: Path = this.cacheDir.resolve(METADATA_FILE_NAME)
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun getArchive(): GtfsArchive {
        require(url.isNotBlank()) { "budatravel.gtfs.bkk-static.url must be set when budatravel.gtfs.source=bkk-static" }
        Files.createDirectories(cacheDir)

        val metadata = readMetadata()
        val request = buildRequest(metadata)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        return when (response.statusCode()) {
            304 -> cachedArchive(metadata, "not modified")
            in 200..299 -> {
                response.body().use { input ->
                    val tempArchive = Files.createTempFile(cacheDir, "budapest-gtfs-", ".zip.download")
                    Files.copy(input, tempArchive, StandardCopyOption.REPLACE_EXISTING)
                    Files.move(tempArchive, archivePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                }
                val updatedMetadata = BkkStaticGtfsCacheMetadata.from(response.headers().map())
                writeMetadata(updatedMetadata)
                cachedArchive(updatedMetadata, "downloaded")
            }
            else -> error("BKK static GTFS download failed with HTTP ${response.statusCode()} from $url")
        }
    }

    private fun buildRequest(metadata: BkkStaticGtfsCacheMetadata?): HttpRequest {
        val builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMinutes(5))
            .GET()
            .header("Accept", "application/zip")

        metadata?.etag?.takeIf { it.isNotBlank() }?.let { builder.header("If-None-Match", it) }
        metadata?.lastModified?.takeIf { it.isNotBlank() }?.let { builder.header("If-Modified-Since", it) }

        return builder.build()
    }

    private fun cachedArchive(metadata: BkkStaticGtfsCacheMetadata?, reason: String): GtfsArchive {
        require(Files.isRegularFile(archivePath)) {
            "BKK static GTFS cache is empty and remote returned no archive: $archivePath"
        }

        return ResourceGtfsArchive(
            resource = FileSystemResource(archivePath),
            source = "bkk-static",
            datasetName = "bkk-static",
            description = "bkk-static:$archivePath ($reason, etag=${metadata?.etag ?: "unknown"})",
            metadata = metadata?.toArchiveMetadata(),
        )
    }

    private fun readMetadata(): BkkStaticGtfsCacheMetadata? {
        if (!Files.isRegularFile(metadataPath)) return null

        val properties = Properties()
        Files.newInputStream(metadataPath).use(properties::load)
        return BkkStaticGtfsCacheMetadata(
            etag = properties.getProperty("etag"),
            lastModified = properties.getProperty("lastModified"),
            contentLength = properties.getProperty("contentLength")?.toLongOrNull(),
            downloadedAt = properties.getProperty("downloadedAt"),
        )
    }

    private fun writeMetadata(metadata: BkkStaticGtfsCacheMetadata) {
        val properties = Properties().apply {
            metadata.etag?.let { setProperty("etag", it) }
            metadata.lastModified?.let { setProperty("lastModified", it) }
            metadata.contentLength?.let { setProperty("contentLength", it.toString()) }
            setProperty("downloadedAt", metadata.downloadedAt ?: Instant.now().toString())
        }
        Files.newOutputStream(metadataPath).use { output ->
            properties.store(output, "BKK static GTFS archive cache metadata")
        }
    }

    companion object {
        private const val ARCHIVE_FILE_NAME = "budapest_gtfs.zip"
        private const val METADATA_FILE_NAME = "budapest_gtfs.properties"
    }
}

data class BkkStaticGtfsCacheMetadata(
    val etag: String?,
    val lastModified: String?,
    val contentLength: Long?,
    val downloadedAt: String?,
) {
    fun toArchiveMetadata(): GtfsArchiveMetadata = GtfsArchiveMetadata(
        etag = etag,
        lastModified = lastModified,
        contentLength = contentLength,
        downloadedAt = downloadedAt,
    )

    companion object {
        fun from(headers: Map<String, List<String>>): BkkStaticGtfsCacheMetadata {
            return BkkStaticGtfsCacheMetadata(
                etag = headers.firstValue("etag"),
                lastModified = headers.firstValue("last-modified"),
                contentLength = headers.firstValue("content-length")?.toLongOrNull(),
                downloadedAt = Instant.now().toString(),
            )
        }

        private fun Map<String, List<String>>.firstValue(name: String): String? = entries
            .firstOrNull { (key, _) -> key.equals(name, ignoreCase = true) }
            ?.value
            ?.firstOrNull()
    }
}
