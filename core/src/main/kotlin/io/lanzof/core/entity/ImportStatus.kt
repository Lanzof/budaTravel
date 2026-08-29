package io.lanzof.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "import_status")
data class ImportStatus(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "dataset_name", nullable = false)
    var datasetName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ImportStatusValue,

    @Column(name = "started_at", nullable = false)
    var startedAt: OffsetDateTime,

    @Column(name = "completed_at")
    var completedAt: OffsetDateTime? = null,

    @Column(name = "locations_count", nullable = false)
    var locationsCount: Long = 0,

    @Column(name = "connections_count", nullable = false)
    var connectionsCount: Long = 0,

    @Column(name = "error_message")
    var errorMessage: String? = null,

    @Column(name = "source")
    var source: String? = null,

    @Column(name = "archive_etag")
    var archiveEtag: String? = null,

    @Column(name = "archive_last_modified")
    var archiveLastModified: String? = null,

    @Column(name = "archive_content_length")
    var archiveContentLength: Long? = null,

    @Column(name = "archive_downloaded_at")
    var archiveDownloadedAt: String? = null,
)

enum class ImportStatusValue {
    RUNNING,
    COMPLETED,
    FAILED,
}
