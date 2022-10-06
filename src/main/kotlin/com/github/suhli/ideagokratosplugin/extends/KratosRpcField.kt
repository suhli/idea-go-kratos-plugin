package com.github.suhli.ideagokratosplugin.extends

import com.intellij.protobuf.lang.psi.PbMessageBody
import com.intellij.protobuf.lang.psi.PbTypeName
import javax.annotation.Nullable

class KratosRpcField(val name: String, val isRepeat: Boolean, @Nullable val children: List<KratosRpcField>?) {
    companion object{
        public fun getFieldsInMessageTypeName(name: PbTypeName): List<KratosRpcField> {
            val fields = arrayListOf<KratosRpcField>()
            val msg: PbMessageBody =
                (name.effectiveReference?.resolve()?.children?.find { v -> v is PbMessageBody }
                    ?: return fields) as PbMessageBody
            for (field in msg.simpleFieldList) {
                if (field.typeName.isBuiltInType) {
                    fields.add(KratosRpcField(field.name!!, field.isRepeated, null))
                } else {
                    fields.add(
                        KratosRpcField(
                            field.name!!,
                            field.isRepeated,
                            getFieldsInMessageTypeName(field.typeName)
                        )
                    )
                }
            }
            return fields
        }
    }
    fun toJson(intentLength: Int = 2): String {
        var intent = ""
        for(i in 1..intentLength){
            intent += " "
        }
        var value = "\"\""
        if (isRepeat) {
            value = "[]"
        } else if (children != null) {
            value = "{\n" + children.map { v -> v.toJson(intentLength + 2) }.joinToString(",\n") + "\n$intent}"
        }
        return "$intent\"$name\":${value}"
    }
}