package io.lanzof.api.handler

import io.lanzof.api.dto.ApiErrorResponse
import io.lanzof.api.exception.InvalidRequestException
import io.lanzof.api.exception.InvalidQueryParametersException
import io.lanzof.api.exception.LocationNotFoundException
import io.lanzof.api.exception.NoRouteFoundException
import io.lanzof.core.service.StopNotFoundException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiErrorHandler {
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(ex: HandlerMethodValidationException): ResponseEntity<ApiErrorResponse> {
        val message = ex.allValidationResults
            .flatMap { it.resolvableErrors }
            .mapNotNull { it.defaultMessage }
            .firstOrNull()
            ?: "Invalid query parameters."
        return badRequest(message)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ApiErrorResponse> {
        val message = ex.constraintViolations.firstOrNull()?.message ?: "Invalid query parameters."
        return badRequest(message)
    }

    @ExceptionHandler(InvalidQueryParametersException::class)
    fun handleInvalidQueryParameters(ex: InvalidQueryParametersException): ResponseEntity<ApiErrorResponse> {
        return badRequest(ex.message ?: "Invalid query parameters.")
    }

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequest(ex: InvalidRequestException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiErrorResponse(
                    code = "INVALID_REQUEST",
                    message = ex.message ?: "Invalid request.",
                )
            )
    }

    @ExceptionHandler(LocationNotFoundException::class)
    fun handleLocationNotFound(ex: LocationNotFoundException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiErrorResponse(
                    code = "LOCATION_NOT_FOUND",
                    message = ex.message ?: "Location not found.",
                )
            )
    }

    @ExceptionHandler(StopNotFoundException::class)
    fun handleStopNotFound(ex: StopNotFoundException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiErrorResponse(
                    code = "LOCATION_NOT_FOUND",
                    message = ex.message ?: "Location not found.",
                )
            )
    }

    @ExceptionHandler(NoRouteFoundException::class)
    fun handleNoRouteFound(ex: NoRouteFoundException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                ApiErrorResponse(
                    code = "NO_ROUTE_FOUND",
                    message = ex.message ?: "No route found for the given parameters.",
                )
            )
    }

    private fun badRequest(message: String): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiErrorResponse(
                    code = "INVALID_QUERY_PARAMETERS",
                    message = message,
                )
            )
    }
}
