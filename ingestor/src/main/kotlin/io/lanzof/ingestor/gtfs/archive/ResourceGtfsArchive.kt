package io.lanzof.ingestor.gtfs.archive

import org.springframework.core.io.Resource
import java.io.InputStream
import java.util.zip.ZipInputStream

class ResourceGtfsArchive(
    private val resource: Resource,
    override val description: String,
) : GtfsArchive {
    override fun openEntry(name: String): InputStream? {
        val zipInputStream = ZipInputStream(resource.inputStream)
        while (true) {
            val entry = zipInputStream.nextEntry
            if (entry == null) {
                zipInputStream.close()
                return null
            }
            if (!entry.isDirectory && entry.name == name) {
                return zipInputStream
            }
        }
    }
}
