package net.azisaba.ryuzupluginchatdiscord.util

import com.github.ucchyocean.lc3.channel.Channel
import com.github.ucchyocean.lc3.member.ChannelMember
import java.util.UUID

fun Channel.containsMember(uuid: UUID) = members.contains(ChannelMember.getChannelMember("$$uuid"))
