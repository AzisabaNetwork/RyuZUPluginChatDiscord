package net.azisaba.ryuzupluginchatdiscord.commands

import com.github.ucchyocean.lc3.LunaChat
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TopGuildChannel
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.rest.builder.interaction.GlobalMultiApplicationCommandBuilder
import dev.kord.rest.builder.interaction.string
import net.azisaba.ryuzupluginchatdiscord.util.DatabaseManager
import net.azisaba.ryuzupluginchatdiscord.util.containsMember
import net.azisaba.ryuzupluginchatdiscord.util.getMinecraftIdName
import net.azisaba.ryuzupluginchatdiscord.util.optString

@Suppress("SqlResolve", "SqlNoDataSourceInspection")
object ConnectCommand : CommandHandler {
    override suspend fun handle0(interaction: ApplicationCommandInteraction) {
        val defer = interaction.deferPublicResponse()
        val channel = interaction.channel.asChannel() as TopGuildChannel
        if (!channel.getEffectivePermissions(interaction.kord.selfId).contains(Permission.ManageWebhooks)) {
            defer.respond { content = "BotがこのチャンネルにWebhookを作成する権限がありません。" }
            return
        }
        val (minecraftUuid) = interaction.user.getMinecraftIdName() ?: run {
            defer.respond { content = "Minecraftアカウントが連携されていません。" }
            return
        }
        val channelName = interaction.optString("channel")!!
        val lcChannel = LunaChat.getAPI().getChannel(channelName)
        if (lcChannel == null) {
            defer.respond { content = "チャンネル`$channelName`に参加していません。" }
            return
        }
        if (!lcChannel.containsMember(minecraftUuid)) {
            defer.respond { content = "チャンネル`$channelName`に参加していません。" }
            return
        }
        val linkedChannelName = DatabaseManager.query("SELECT `lc_channel_name` FROM `channels` WHERE `channel_id` = ?") {
            it.setString(1, channelName)
            it.executeQuery().use { rs ->
                if (rs.next()) {
                    rs.getLong("guild_id")
                } else {
                    null
                }
            }
        }
        if (linkedChannelName != null) {
            defer.respond {
                content = "このチャンネルは既にチャンネル`$linkedChannelName`と連携されています。\n`/disconnect`で連携を解除できます。"
            }
            return
        }
        val webhook = channel.kord.rest.webhook.createWebhook(channel.id, "チャンネルチャット ($channelName)") {
            reason = "/connect command from ${interaction.user.tag} (${interaction.user.id})"
        }
        DatabaseManager.query("INSERT INTO `channels` (`lc_channel_name`, `channel_id`, `webhook_id`, `webhook_token`, `created_user_id`) VALUES (?, ?, ?, ?, ?)") {
            it.setString(1, channelName)
            it.setLong(2, channel.id.value.toLong())
            it.setLong(3, webhook.id.value.toLong())
            it.setString(4, webhook.token.value)
            it.setLong(5, interaction.user.id.value.toLong())
            it.executeUpdate()
        }
        DatabaseManager.getChannelNameByChannelId.forget(channel.id.value.toLong())
        DatabaseManager.getWebhooksByChannelName.forget(channelName)
        defer.respond { content = "チャンネルチャットを`$channelName`と連携しました。\nメッセージが受信できるようになるまで最大60秒程度かかります。" }
    }

    override fun register(builder: GlobalMultiApplicationCommandBuilder) {
        builder.input("connect", "チャンネルチャットを連携します") {
            dmPermission = false
            defaultMemberPermissions = Permissions(Permission.ManageWebhooks)
            string("channel", "チャンネル名") {
                required = true
            }
        }
    }
}
