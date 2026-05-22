package com.flue.launcher.iconpack

data class IconPackDescriptor(
    val packageName: String,
    val label: String
)

data class IconPackMapping(
    val descriptor: IconPackDescriptor,
    val componentToDrawable: Map<String, String>,
    val template: IconPackTemplate = IconPackTemplate()
)

data class IconPackTemplate(
    val iconBacks: List<String> = emptyList(),
    val iconMask: String? = null,
    val iconUpon: String? = null,
    val scale: Float = 1f
) {
    val hasTemplate: Boolean
        get() = iconBacks.isNotEmpty() || iconMask != null || iconUpon != null
}
