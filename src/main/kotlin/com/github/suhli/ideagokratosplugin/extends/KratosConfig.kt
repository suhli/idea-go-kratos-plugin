package com.github.suhli.ideagokratosplugin.extends

import com.intellij.openapi.diagnostic.Logger

class KratosConfig {
    public var wireLocation = ""
    public var layoutRepository = ""

    companion object {
        private val LOG = Logger.getInstance(KratosConfig::class.java)
        fun fromLines(lines: List<String>): KratosConfig {
            val config = KratosConfig()
            for (line in lines) {
                if (!line.contains("=")) {
                    continue
                }
                val (k, v) = line.split("=")
                try {
                    val field = KratosConfig::class.java.getDeclaredField(k)
                    field.trySetAccessible()
                    field.set(config, v)
                } catch (e: Exception) {
                    LOG.error(e)
                }
            }
            return config
        }
    }
}