package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class OdgerWaypoint {
    private val location = LorenzVec(-373, 207, -808)

    private var tick = 0
    private var hasLavaRodInHand = false
    private var trophyFishInInv = false

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!isEnabled()) return

        tick++
        if (tick % 10 == 0) {
            hasLavaRodInHand = isLavaFishingRod()

            trophyFishInInv = InventoryUtils.getItemsInOwnInventory().map { it.getLore() }
                .any { it.isNotEmpty() && it.last().endsWith("TROPHY FISH") }
        }
    }

    private fun isLavaFishingRod(): Boolean {
        val heldItem = InventoryUtils.getItemInHand() ?: return false
        val isRod = heldItem.name?.contains("Rod") ?: return false
        if (!isRod) return false

        return heldItem.getLore().any { it.contains("Lava Rod") }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isEnabled()) return
        if (hasLavaRodInHand) return
        if (!trophyFishInInv) return

        event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
        event.drawDynamicText(location, "Odger", 1.5)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.fishing.odgerLocation &&
            LorenzUtils.skyBlockIsland == IslandType.CRIMSON_ISLE
}