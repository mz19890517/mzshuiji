package com.mz.shunji.preferences

interface HasNameResource {
    val nameResource: Int
}

interface HasSupportRequirement {
    fun isSupported() = true
}