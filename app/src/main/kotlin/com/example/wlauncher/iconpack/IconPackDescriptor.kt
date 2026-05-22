package com.flue.launcher.iconpack

data class IconPackDescriptor(
    val packageName: String,
    val label: String
)

data class IconPackMapping(
    val descriptor: IconPackDescriptor,
    val componentToDrawable: Map<String, String>
)
