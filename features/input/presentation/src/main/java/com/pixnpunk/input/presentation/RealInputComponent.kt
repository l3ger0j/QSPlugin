package com.pixnpunk.input.presentation

import com.arkivanov.decompose.ComponentContext

class RealInputComponent(
    private val componentContext: ComponentContext
) : ComponentContext by componentContext, InputComponent {
}