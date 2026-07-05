package io.lanzof.core.service

import io.lanzof.core.entity.ImportStatus
import io.lanzof.core.entity.ImportStatusValue
import io.lanzof.core.repo.ConnectionRepo
import io.lanzof.core.repo.ImportStatusRepo
import io.lanzof.core.repo.LocationRepo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ImportStatusService(
    private val importStatusRepo: ImportStatusRepo,
    private val locationRepo: LocationRepo,
    private val connectionRepo: ConnectionRepo,
) {
    @Transactional
    fun markRunning(datasetName: String = DEMO_DATASET_NAME): ImportStatus {
        return importStatusRepo.save(
            ImportStatus(
                datasetName = datasetName,
                status = ImportStatusValue.RUNNING,
                startedAt = OffsetDateTime.now(),
            )
        )
    }

    @Transactional
    fun markCompleted(datasetName: String = DEMO_DATASET_NAME): ImportStatus {
        val status = latestOrNew(datasetName)
        status.status = ImportStatusValue.COMPLETED
        status.completedAt = OffsetDateTime.now()
        status.locationsCount = locationRepo.count()
        status.connectionsCount = connectionRepo.count()
        status.errorMessage = null
        return importStatusRepo.save(status)
    }

    @Transactional
    fun markFailed(datasetName: String = DEMO_DATASET_NAME, error: Throwable): ImportStatus {
        val status = latestOrNew(datasetName)
        status.status = ImportStatusValue.FAILED
        status.completedAt = OffsetDateTime.now()
        status.locationsCount = locationRepo.count()
        status.connectionsCount = connectionRepo.count()
        status.errorMessage = error.message?.take(MAX_ERROR_MESSAGE_LENGTH) ?: error::class.simpleName
        return importStatusRepo.save(status)
    }

    @Transactional(readOnly = true)
    fun getReadiness(datasetName: String = DEMO_DATASET_NAME): DataReadiness {
        val status = importStatusRepo.findFirstByDatasetNameOrderByStartedAtDesc(datasetName)
        val locationsCount = locationRepo.count()
        val connectionsCount = connectionRepo.count()
        val ready = status?.status == ImportStatusValue.COMPLETED && locationsCount > 0 && connectionsCount > 0

        return DataReadiness(
            datasetName = datasetName,
            ready = ready,
            status = status?.status,
            startedAt = status?.startedAt,
            completedAt = status?.completedAt,
            locationsCount = locationsCount,
            connectionsCount = connectionsCount,
            errorMessage = status?.errorMessage,
        )
    }

    private fun latestOrNew(datasetName: String): ImportStatus {
        return importStatusRepo.findFirstByDatasetNameOrderByStartedAtDesc(datasetName)
            ?: ImportStatus(
                datasetName = datasetName,
                status = ImportStatusValue.RUNNING,
                startedAt = OffsetDateTime.now(),
            )
    }

    companion object {
        const val DEMO_DATASET_NAME = "budapest-mini"
        private const val MAX_ERROR_MESSAGE_LENGTH = 2_000
    }
}
