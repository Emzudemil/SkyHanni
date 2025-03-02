package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class ServerRestartTitle {
    private val config get() = SkyHanniMod.feature.misc
    private var tick = 0
    private val pattern = "§cServer closing: (?<minutes>\\d+):(?<seconds>\\d+) §8.*".toPattern()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.serverRestartTitle) return

        if (event.phase != TickEvent.Phase.START) return
        tick++

        if (tick % 20 != 0) return

        for (line in ScoreboardData.sidebarLinesFormatted) {
            pattern.matchMatcher(line) {
                val minutes = group("minutes").toInt()
                val seconds = group("seconds").toInt()
                val totalSeconds = minutes * 60 + seconds
                val time = TimeUtils.formatDuration(totalSeconds.toLong() * 1000)
                TitleUtils.sendTitle("§cServer Restart in §b$time", 2_000)
            }
        }
    }
}
