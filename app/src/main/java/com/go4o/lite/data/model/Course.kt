package com.go4o.lite.data.model

data class Course(
    val name: String,
    val controls: List<Int>,
    val length: Int? = null,
    val climb: Int? = null
)
