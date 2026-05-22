package com.flue.launcher.watchface

import java.util.concurrent.ConcurrentHashMap

object LunchWatchFaceRegistry {
    private val descriptors = ConcurrentHashMap<String, LunchWatchFaceDescriptor>()
    @Volatile
    private var currentSelectedId: String = BUILT_IN_WATCHFACE_ID

    @JvmStatic
    fun update(all: List<LunchWatchFaceDescriptor>) {
        descriptors.clear()
        all.forEach { descriptor ->
            descriptors[descriptor.id] = descriptor
            descriptor.packageName?.let { descriptors.putIfAbsent(it, descriptor) }
            descriptor.watchFaceName.let { descriptors.putIfAbsent(it, descriptor) }
        }
    }

    @JvmStatic
    fun setCurrentSelectedId(id: String?) {
        currentSelectedId = id ?: BUILT_IN_WATCHFACE_ID
    }

    @JvmStatic
    fun getCurrentSelected(): LunchWatchFaceDescriptor? = descriptors[currentSelectedId]

    @JvmStatic
    fun resolve(identifier: String?): LunchWatchFaceDescriptor? {
        if (identifier.isNullOrBlank()) return getCurrentSelected()
        return descriptors[identifier] ?: descriptors.values.firstOrNull {
            it.id == identifier || it.packageName == identifier || it.watchFaceName == identifier
        }
    }
}
