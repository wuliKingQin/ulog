package com.utopia.android.ulog.print.format

import org.json.JSONArray
import org.json.JSONObject

/**
 * des: json字符串进行格式化
 * author 秦王
 * time 2021/12/1 11:48
 */
class JsonFormatter: Formatter<String>{

    companion object {
        private const val JSON_INDENT = 4
    }

    override fun format(data: String): String {
        if (data.isEmpty()) {
            return data
        }
        return try {
            when {
                data.startsWith("{") -> {
                    JSONObject(data).toString(JSON_INDENT)
                }
                data.startsWith("[") -> {
                    JSONArray(data).toString(JSON_INDENT)
                }
                else -> {
                    data
                }
            }
        } catch (e: Exception) {
            return data
        }
    }
}