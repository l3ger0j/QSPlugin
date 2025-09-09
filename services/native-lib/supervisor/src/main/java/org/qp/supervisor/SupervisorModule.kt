package org.qp.supervisor

import org.koin.dsl.module

val supervisorModule = module {
    single { SupervisorService(get()) }
}