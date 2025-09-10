package org.qp.supervisor

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val supervisorModule = module {
    singleOf(::SupervisorService)
}