package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RiftAgaricusCap {
    private val config get() = SkyHanniMod.feature.rift
    private var startTime = 0L
    private var location: LorenzVec? = null

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        val area = LorenzUtils.skyBlockArea
        if (area != "West Village" && area != "Dreadfarm") return

        location = updateLocation()
    }

    private fun updateLocation(): LorenzVec? {
        if (InventoryUtils.getItemInHand()?.getInternalName() != "FARMING_WAND") return null
        val currentLocation = BlockUtils.getBlockLookingAt() ?: return null

        when (currentLocation.getBlockStateAt().toString()) {
            "minecraft:brown_mushroom" -> {
                return if (location != currentLocation) {
                    startTime = System.currentTimeMillis()
                    currentLocation
                } else {
                    if (startTime == -1L) {
                        startTime = System.currentTimeMillis()
                    }
                    location
                }
            }

            "minecraft:red_mushroom" -> {
                if (location == currentLocation) {
                    startTime = -1L
                    return location
                }
            }
        }
        return null
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isEnabled()) return

        val location = location?.add(0.0, 0.6, 0.0) ?: return

        if (startTime == -1L) {
            event.drawDynamicText(location, "§cClick!", 1.5)
            return
        }

        val countDown = System.currentTimeMillis() - startTime
        val format = TimeUtils.formatDuration(countDown - 1000, showMilliSeconds = true)
        event.drawDynamicText(location, "§b$format", 1.5)
    }

    fun isEnabled() = RiftAPI.inRift() && config.agaricusCap
}
