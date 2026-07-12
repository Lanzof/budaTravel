package io.lanzof.ingestor.gtfs.archive

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component

@Component
class ConfigurableGtfsArchiveProvider(
    private val resourceLoader: ResourceLoader,
    private val bkkStaticProvider: BkkStaticGtfsArchiveProvider,
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
            "bkk-static" -> return bkkStaticProvider.getArchive()
            else -> throw IllegalArgumentException(
                "Unsupported budatravel.gtfs.source='$source'. Supported values: demo, local-file, bkk-static."
            )
        }

        val resource = resourceLoader.getResource(resourceLocation)
        require(resource.exists()) { "GTFS archive resource does not exist: $resourceLocation" }

        return ResourceGtfsArchive(
            resource = resource,
            source = source,
            datasetName = source.datasetName(),
            description = "$source:$resourceLocation",
        )
    }
}


private fun String.datasetName(): String = when (this) {
    "demo" -> "budapest-mini"
    "local-file" -> "local-gtfs"
    else -> this
}
