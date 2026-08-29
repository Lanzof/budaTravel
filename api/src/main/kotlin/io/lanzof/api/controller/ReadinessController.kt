package io.lanzof.api.controller

import io.lanzof.api.dto.ReadinessResponse
import io.lanzof.core.service.DataReadiness
import io.lanzof.core.service.ImportStatusService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/readiness")
@Tag(name = "Readiness", description = "Runtime readiness endpoints")
class ReadinessController(
    private val importStatusService: ImportStatusService,
) {
    @GetMapping
    @Operation(summary = "Check whether imported GTFS data is ready for UI traffic")
    fun readiness(): ResponseEntity<ReadinessResponse> {
        val readiness = importStatusService.getReadiness()
        val status = if (readiness.ready) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
        return ResponseEntity.status(status).body(readiness.toResponse())
    }

    private fun DataReadiness.toResponse(): ReadinessResponse = ReadinessResponse(
        datasetName = datasetName,
        ready = ready,
        status = status,
        startedAt = startedAt,
        completedAt = completedAt,
        locationsCount = locationsCount,
        connectionsCount = connectionsCount,
        errorMessage = errorMessage,
        source = source,
        archiveEtag = archiveEtag,
        archiveLastModified = archiveLastModified,
        archiveContentLength = archiveContentLength,
    )
}
