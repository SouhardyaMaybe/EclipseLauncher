package me.shadow.eclipselauncher.feature.notice

import me.shadow.eclipselauncher.json.annotations.SerializedName

class NoticeJsonObject(
    val title: Text = Text(),
    val content: Text = Text(),
    val date: String = "",
    val numbering: Int = 0
) {
    class Text(
        @SerializedName("en_us") val enUS: String = ""
    )
}