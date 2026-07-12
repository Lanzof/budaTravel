package io.lanzof.ingestor.gtfs.archive

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component

@Component
class ConfigurableGtfsArchiveProvider(
    private val resourceLoader: ResourceLoader,
    @param:Value("\${budatravel.gtfs.source}") private val source: String,
    @param:Value("\${budatravel.gtfs.demo.resource}") private val demoResource: String,
    @param:Value("\${budatravel.gtfs.local-file.path}") private val localFilePath: String,
) : GtfsArchiveProvider {
    override fun getArchive(): GtfsArchive {
        val resourceLocation = when (source) {
            "demo" -> demoResource
            "local-file" -> {
                require(localFilePath.isNotBlank()) {
                    "budatravel.gtfs.local-file.path must be set when budatravel.gtfs.source=local-file"
                }
                "file:$localFilePath"
            }
            "bkk-remote" -> throw UnsupportedOperationException(
                "budatravel.gtfs.source=bkk-remote is reserved, but the remote BKK archive provider is not implemented yet."
            )
            else -> throw IllegalArgumentException(
                "Unsupported budatravel.gtfs.source='$source'. Supported values: demo, local-file, bkk-remote."
            )
        }

        val resource = resourceLoader.getResource(resourceLocation)
        require(resource.exists()) { "GTFS archive resource does not exist: $resourceLocation" }

        return ResourceGtfsArchive(
            resource = resource,
            description = "$source:$resourceLocation",
        )
    }
}
