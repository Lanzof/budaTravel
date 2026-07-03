package io.lanzof.core.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@ComponentScan("io.lanzof.core")
@EntityScan(basePackages = ["io.lanzof.core.entity"])
@EnableJpaRepositories(basePackages = ["io.lanzof.core.repo"])
class CoreConfig {
}