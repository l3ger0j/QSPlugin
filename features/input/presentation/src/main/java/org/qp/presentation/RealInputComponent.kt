package org.qp.presentation

import com.arkivanov.decompose.ComponentContext

class RealInputComponent(
    private val componentContext: ComponentContext
) : ComponentContext by componentContext, InputComponent {
}