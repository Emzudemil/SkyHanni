package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemStars {

    private val armorNames = mutableListOf<String>()
    private val tiers = mutableMapOf<String, Int>()
    private val STAR_FIND_PATCHER = "(.*)§.✪(.*)".toPattern()
    private val armorParts = listOf("Helmet", "Chestplate", "Leggings", "Boots")

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onTooltip(event: ItemTooltipEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val stack = event.itemStack ?: return
        if (stack.stackSize != 1) return
        if (!SkyHanniMod.feature.inventory.itemStars) return

        val itemName = stack.name ?: return
        val stars = getStars(itemName)

        if (stars > 0) {
            var name = itemName
            while (STAR_FIND_PATCHER.matcher(name).matches()) {
                name = name.replaceFirst("§.✪".toRegex(), "")
            }
            name = name.trim()
            event.toolTip[0] = "$name §c$stars✪"
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        try {
            val items = event.getConstant("Items")!!
            if (items.has("crimson_armors")) {
                armorNames.clear()
                armorNames.addAll(items.getAsJsonArray("crimson_armors").map { it.asString })
            }

            tiers.clear()
            if (items.has("crimson_tiers")) {
                items.getAsJsonObject("crimson_tiers").entrySet().forEach {
                    tiers[it.key] = it.value.asInt
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(6)) return
        val stack = event.stack
        val number = getCrimsonStars(stack.name ?: return)
        if (number != -1) {
            event.stackTip = number.toString()
        }
    }

    private fun getStars(name: String): Int {
        val stars = getCrimsonStars(name)
        if (stars != -1) {
            return stars
        }

        return getOtherStars(name)
    }

    private fun getCrimsonStars(name: String): Int {
        if (!armorNames.any { name.contains(it) } || !armorParts.any { name.contains(it) }) {
            return -1
        }
        var name1 = name
        var gold = 0
        var pink = 0
        var aqua = 0
        while (name1.contains("§6✪")) {
            name1 = name1.replaceFirst("§6✪", "")
            gold++
        }
        while (name1.contains("§d✪")) {
            name1 = name1.replaceFirst("§d✪", "")
            pink++
        }
        while (name1.contains("§b✪")) {
            name1 = name1.replaceFirst("§b✪", "")
            aqua++
        }
        return (tiers.entries.find { name1.contains(it.key) }?.value ?: 0) + if (aqua > 0) {
            10 + aqua
        } else if (pink > 0) {
            5 + pink
        } else {
            gold
        }
    }

    private fun getOtherStars(originalName: String): Int {
        var name = originalName

        var gold = 0
        var red = 0
        while (name.contains("§6✪")) {
            name = name.replaceFirst("§6✪", "")
            gold++
        }
        while (name.contains("§c✪")) {
            name = name.replaceFirst("§c✪", "")
            red++
        }
        while (name.contains("§d✪")) {
            name = name.replaceFirst("§d✪", "")
            red++
        }

        if (red > 0) return 5 + red
        if (gold > 0) return gold

        return -1
    }
}