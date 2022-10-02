package com.github.suhli.ideagokratosplugin.extends

class KratosConfig {
    public var wireLocation = ""
    companion object {
        fun fromLines(lines:List<String>): KratosConfig {
            val config = KratosConfig()
            for(line in lines){
                if(!line.contains("=")){
                    continue
                }
                val (k,v) = line.split("=")
                try {
                    val field = KratosConfig::class.java.getDeclaredField(k)
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