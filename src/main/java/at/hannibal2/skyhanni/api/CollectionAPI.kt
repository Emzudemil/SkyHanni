package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class CollectionAPI {
    private val counterPattern = Pattern.compile("(?:.*) §e(.*)§6\\/(?:.*)")
    private val singleCounterPattern = Pattern.compile("§7Total Collected: §e(.*)")

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        val profileData = event.profileData
        for ((rawName, rawCounter) in profileData["collection"].asJsonObject.entrySet()) {
            val counter = rawCounter.asLong
            var itemName = BazaarApi.getBazaarDataForInternalName(rawName)?.itemName
            if (rawName == "MUSHROOM_COLLECTION") {
                itemName = "Mushroom"
            }
            if (rawName == "MELON") {
                itemName = "Melon"
            }
            if (rawName == "GEMSTONE_COLLECTION") {
                itemName = "Gemstone"
            }
            // Hypixel moment
            if (rawName == "WOOL" || rawName == "CORRUPTED_FRAGMENT") {
                continue
            }
            if (itemName == null) {
                println("collection name is null for '$rawName'")
                continue
            }
            collectionValue[itemName] = counter
        }
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        collectionValue.clear()
    }

    @SubscribeEvent
    fun onTick(event: InventoryOpenEvent) {
        val inventoryName = event.inventoryName
        if (inventoryName.endsWith(" Collection")) {
            val stack = event.inventoryItems[4] ?: return
            for (line in stack.getLore()) {
                val matcher = singleCounterPattern.matcher(line)
                if (matcher.matches()) {
                    val counter = matcher.group(1).replace(",", "").toLong()
                    val name = inventoryName.split(" ").dropLast(1).joinToString(" ")
                    collectionValue[name] = counter
                }
            }
        }

        if (inventoryName.endsWith(" Collections")) {
            if (inventoryName == "Boss Collections") return

            for ((_, stack) in event.inventoryItems) {
                var name = stack.name?.removeColor() ?: continue
                if (name.contains("Collections")) continue

                val lore = stack.getLore()
                if (!lore.any { it.contains("Click to view!") }) continue

                if (!isCollectionTier0(lore)) {
                    name = name.split(" ").dropLast(1).joinToString(" ")
                }

                for (line in lore) {
                    val matcher = counterPattern.matcher(line)
                    if (matcher.matches()) {
                        val counter = matcher.group(1).replace(",", "").toLong()
                        collectionValue[name] = counter
                    }
                }
            }
        }
    }

    companion object {
        private val collectionValue = mutableMapOf<String, Long>()
        private val collectionTier0Pattern = Pattern.compile("§7Progress to .* I: .*")

        fun isCollectionTier0(lore: List<String>) = lore.map { collectionTier0Pattern.matcher(it) }.any { it.matches() }

        fun getCollectionCounter(searchName: String): Pair<String, Long>? {
            for ((collectionName, counter) in collectionValue) {
                if (collectionName.equals(searchName, true)) {
                    return Pair(collectionName, counter)
                }
            }
            return null
        }
    }
}