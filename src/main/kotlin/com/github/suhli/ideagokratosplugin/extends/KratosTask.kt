package com.github.suhli.ideagokratosplugin.extends


class KratosTaskResult {
    companion object{

        fun dismiss():KratosTaskResult{
            val res = KratosTaskResult()
            res.dismiss = true
            return res
        }
        fun success(): KratosTaskResult {
            return KratosTaskResult()
        }
        fun error(err:RuntimeException): KratosTaskResult {
            val res = KratosTaskResult()
            res.exception = err
            return res
        }
    }

    var dismiss = false;

    var exception: RuntimeException? = null
    var message: String? = null
    get(): String? {
        if (field == null){
            return exception?.message
        }
        return field
    }
}

class KratosTask(val runnable: () -> KratosTaskResult?, val name: String, val needWrite: Boolean = false) {
}