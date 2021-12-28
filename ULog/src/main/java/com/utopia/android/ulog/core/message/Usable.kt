package com.utopia.android.ulog.core.message

import java.util.concurrent.atomic.AtomicInteger

/**
 * des: 定义该类是否可用，用于对象的复用
 * author 秦王
 * time 2021/11/16 14:57
 */
abstract class Usable {

    // doc: 使用标志
    val userFlag = AtomicInteger(-1)

    /**
     * des: 标记该类正在使用中
     * time: 2021/11/16 15:00
     */
    fun makeUse(useValue: Int = 0) {
        if (userFlag.get() < 0) {
            userFlag.set(useValue)
        }
        userFlag.incrementAndGet()
    }

    /**
     * des: 检测是否可用，不可以将结果进行回调，提供销毁对象
     * time: 2021/11/16 14:59
     */
    protected inline fun checkUse(action: ()-> Unit) {
        if (userFlag.get() > 0) {
            userFlag.decrementAndGet()
        }
        if (userFlag.get() <= 0) {
            userFlag.set(-1)
            action.invoke()
        }
    }
}