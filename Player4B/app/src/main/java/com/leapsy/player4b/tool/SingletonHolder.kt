package com.leapsy.player4b.tool

open class SingletonHolder<out T : Any, in A>(creator : (A) -> T) {
    private var creator : ((A) -> T)? = creator
    @Volatile private var mInstance : T? = null

    fun getInstance(arg: A) : T {
        val checkInstance  = mInstance
        if (checkInstance != null) {
            return checkInstance
        }

        return synchronized(this) {
            val checkInstanceAgain = mInstance
            if (checkInstanceAgain != null) {
                checkInstanceAgain
            } else {
                val created = creator!!(arg)
                mInstance = created
                creator = null
                created
            }
        }
    }
}