package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit

class ComposterDisplay {
    private val config get() = SkyHanniMod.feature.garden
    private val hidden get() = GardenAPI.config
    private var display = listOf<List<Any>>()
    private var composterEmptyTime: Duration? = null

    private var tabListData by ComposterAPI::tabListData

    enum class DataType(rawPattern: String, val icon: String) {
        ORGANIC_MATTER(" Organic Matter: §r(.*)", "WHEAT"),
        FUEL(" Fuel: §r(.*)", "OIL_BARREL"),
        TIME_LEFT(" Time Left: §r(.*)", "WATCH"),
        STORED_COMPOST(" Stored Compost: §r(.*)", "COMPOST");

        val displayItem by lazy {
            NEUItems.getItemStack(icon)
        }

        val pattern by lazy { rawPattern.toPattern() }

        fun addToList(map: Map<DataType, String>): List<Any> {
            return listOf(displayItem, map[this]!!)
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!(config.composterDisplayEnabled && GardenAPI.inGarden())) return

        readData(event.tabList)

        if (tabListData.isNotEmpty()) {
            calculateEmptyTime()
            updateDisplay()
            sendNotify()
        }
    }

    private fun calculateEmptyTime() {
        val organicMatter = ComposterAPI.getOrganicMatter()
        val fuel = ComposterAPI.getFuel()

        if (ComposterAPI.composterUpgrades.isNullOrEmpty()) {
            composterEmptyTime = null
            return
        }

        val timePerCompost = ComposterAPI.timePerCompost(null)

        val organicMatterRequired = ComposterAPI.organicMatterRequiredPer(null)
        val fuelRequired = ComposterAPI.fuelRequiredPer(null)

        val organicMatterRemaining = organicMatter / organicMatterRequired
        val fuelRemaining = fuel / fuelRequired

        val endOfOrganicMatter = timePerCompost * organicMatterRemaining
        val endOfFuel = timePerCompost * fuelRemaining
        composterEmptyTime = if (endOfOrganicMatter > endOfFuel) endOfFuel else endOfOrganicMatter
    }

    private fun updateDisplay() {
        val newDisplay = mutableListOf<List<Any>>()
        newDisplay.addAsSingletonList("§bComposter")


        newDisplay.add(DataType.TIME_LEFT.addToList(tabListData))

        val list = mutableListOf<Any>()
        list.addAll(DataType.ORGANIC_MATTER.addToList(tabListData))
        list.add(" ")
        list.addAll(DataType.FUEL.addToList(tabListData))
        newDisplay.add(list)

        newDisplay.add(DataType.STORED_COMPOST.addToList(tabListData))
        newDisplay.add(addComposterEmptyTime(composterEmptyTime))

        display = newDisplay
    }

    private fun addComposterEmptyTime(emptyTime: Duration?): List<Any> {
        return if (emptyTime != null) {
            val millis = emptyTime.toDouble(DurationUnit.MILLISECONDS).toLong()
            GardenAPI.config?.composterEmptyTime = System.currentTimeMillis() + millis
            val format = TimeUtils.formatDuration(millis, maxUnits = 2)
            listOf(NEUItems.getItemStack("BUCKET"), "§b$format")
        } else {
            listOf("§cOpen Composter Upgrades!")
        }
    }

    private fun readData(tabList: List<String>) {
        var next = false
        val newData = mutableMapOf<DataType, String>()

        for (line in tabList) {
            if (line == "§b§lComposter:") {
                next = true
                continue
            }
            if (next) {
                if (line == "") break
                for (type in DataType.values()) {
                    type.pattern.matchMatcher(line) {
                        newData[type] = group(1)
                    }
                }
            }
        }

        for (type in DataType.values()) {
            if (!newData.containsKey(type)) {
                tabListData = emptyMap()
                return
            }
        }

        tabListData = newData
    }

    private fun sendNotify() {
        if (!config.composterNotifyLowEnabled) return
        val hidden = hidden ?: return

        if (ComposterAPI.getOrganicMatter() <= config.composterNotifyLowOrganicMatter) {
            if (System.currentTimeMillis() >= hidden.informedAboutLowMatter) {
                if (config.composterNotifyLowTitle) {
                    TitleUtils.sendTitle("§cYour Organic Matter is low", 4_000)
                }
                LorenzUtils.chat("§e[SkyHanni] §cYour Organic Matter is low!")
                hidden.informedAboutLowMatter = System.currentTimeMillis() + 60_000 * 5
            }
        }

        if (ComposterAPI.getFuel() <= config.composterNotifyLowFuel &&
            System.currentTimeMillis() >= hidden.informedAboutLowFuel
        ) {
            if (config.composterNotifyLowTitle) {
                TitleUtils.sendTitle("§cYour Fuel is low", 4_000)
            }
            LorenzUtils.chat("§e[SkyHanni] §cYour Fuel is low!")
            hidden.informedAboutLowFuel = System.currentTimeMillis() + 60_000 * 5
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (GardenAPI.inGarden() && config.composterDisplayEnabled) {
            config.composterDisplayPos.renderStringsAndItems(display, posLabel = "Composter Display")
        }

        checkWarningsAndOutsideGarden()
    }

    private fun checkWarningsAndOutsideGarden() {
        val storage = GardenAPI.config ?: return

        val format = if (storage.composterEmptyTime != 0L) {
            val duration = storage.composterEmptyTime - System.currentTimeMillis()
            if (duration > 0) {
                if (duration < 1000 * 60 * 20) {
                    warn("Your composter in the garden is soon empty!")
                }
                TimeUtils.formatDuration(duration, maxUnits = 3)
            } else {
                warn("Your composter is empty!")
                "§cComposter is empty!"
            }
        } else "?"

        if (!GardenAPI.inGarden() && config.composterDisplayOutsideGarden) {
            val list = Collections.singletonList(listOf(NEUItems.getItemStack("BUCKET"), "§b$format"))
            config.composterOutsideGardenPos.renderStringsAndItems(list, posLabel = "Composter Outside Garden Display")
        }
    }

    private fun warn(warningMessage: String) {
        if (!config.composterWarnAlmostClose) return
        val storage = GardenAPI.config ?: return

        if (LorenzUtils.inDungeons) return
        if (LorenzUtils.inKuudraFight) return

        if (System.currentTimeMillis() < storage.lastComposterEmptyWarningTime + 1000 * 60 * 2) return
        storage.lastComposterEmptyWarningTime = System.currentTimeMillis()
        LorenzUtils.chat("§e[SkyHanni] $warningMessage")
        TitleUtils.sendTitle("§eComposter Warning!", 3_000)
    }
}