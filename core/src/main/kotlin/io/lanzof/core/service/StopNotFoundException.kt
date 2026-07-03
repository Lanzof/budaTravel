package io.lanzof.core.service

class StopNotFoundException(stopId: String) : RuntimeException("Stop '$stopId' not found")

