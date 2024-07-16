package ru.blays.plugins.translate.utils

import com.aliucord.CollectionUtils
import com.discord.widgets.chat.list.WidgetChatList
import com.discord.widgets.chat.list.entries.MessageEntry

fun WidgetChatList.rerenderMessage(messageId: Long) {
    val adapter = WidgetChatList.`access$getAdapter$p`(this)
    val data = adapter.internalData
    val i = CollectionUtils.findIndex(data) { m ->
        m is MessageEntry && m.message.id == messageId
    }
    if (i != -1) adapter.notifyItemChanged(i)
}