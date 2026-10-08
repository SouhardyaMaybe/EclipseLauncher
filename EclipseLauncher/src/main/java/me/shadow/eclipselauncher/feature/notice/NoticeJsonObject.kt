package me.shadow.eclipselauncher.feature.notice

import com.google.gson.annotations.SerializedName

class NoticeJsonObject(
    val title: Text,
    val content: Text,
    val date: String,
    val numbering: Int
) {
    class Text(
        @SerializedName("en_us") val enUS: String
    )
}