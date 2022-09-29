package com.github.suhli.ideagokartosplugin.extends

class WireConfig {
    public var location = ""
    public var additionalDir = ""

    companion object {
        fun fromLines(lines:List<String>): WireConfig {
            val config = WireConfig()
            for(line in lines){
                if(!line.contains("=")){
                    continue
                }
                val (k,v) = line.split("=")
                try {
                    val field = WireConfig::class.java.getDeclaredField(k)
                    field.trySetAccessible()
                    field.set(config,v)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
            return config
        }
    }
}