package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.jsonobjects.ModsJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object QuickModMenuSwitch {
    private val config get() = SkyHanniMod.feature.misc.quickModMenuSwitch
    private var display = listOf<List<Any>>()
    private var tick = 0
    private var latestGuiPath = ""

    private var mods: List<Mod>? = null

    private var currentlyOpeningMod = ""
    private var lastGuiOpen = 0L

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val modsJar = event.getConstant<ModsJson>("ModGuiSwitcher") ?: return
        mods = buildList {
            out@ for ((name, mod) in modsJar.mods) {
                for (path in mod.guiPath) {
                    try {
                        Class.forName(path)
                        add(Mod(name, mod.description, mod.command, mod.guiPath))
                        continue@out
                    } catch (ignored_: Exception) {
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (tick++ % 5 == 0) {
            update()
        }
    }

    class Mod(val name: String, val description: List<String>, val command: String, val guiPath: List<String>) {

        fun isInGui() = guiPath.any { latestGuiPath.startsWith(it) }
    }

    private fun update() {
        var openGui = Minecraft.getMinecraft().currentScreen?.javaClass?.name ?: "none"
        openGui = handleAbstractGuis(openGui)
        if (latestGuiPath != openGui) {
            latestGuiPath = openGui

            if (SkyHanniMod.feature.dev.modMenuLog) {
                LorenzUtils.debug("Open GUI: $latestGuiPath")
            }
        }
        val mods = mods ?: return

        display = if (!shouldShow(mods)) {
            emptyList()
        } else {
            renderDisplay(mods)
        }
    }

    private fun shouldShow(mods: List<Mod>): Boolean {
        if (config.insideEscapeMenu && latestGuiPath == "net.minecraft.client.gui.GuiIngameMenu") return true
        if (config.insidePlayerInventory && latestGuiPath == "net.minecraft.client.gui.inventory.GuiInventory") return true

        return mods.any { it.isInGui() }
    }

    private fun handleAbstractGuis(openGui: String): String {
        if (openGui == "gg.essential.vigilance.gui.SettingsGui") {
            val clazz = Class.forName("gg.essential.vigilance.gui.SettingsGui")
            val titleBarDelegate = clazz.getDeclaredField("titleBar\$delegate").also { it.isAccessible = true }
                .get(Minecraft.getMinecraft().currentScreen)
            val titleBar =
                titleBarDelegate.javaClass.declaredFields[0].also { it.isAccessible = true }.get(titleBarDelegate)
            val gui = titleBar.javaClass.getDeclaredField("gui").also { it.isAccessible = true }.get(titleBar)
            val config = gui.javaClass.getDeclaredField("config").also { it.isAccessible = true }.get(gui)

            return config.javaClass.name
        }
        if (openGui == "cc.polyfrost.oneconfig.gui.OneConfigGui") {
            /** TODO support different oneconfig mods:
             * Partly Sane Skies
             * Dankers SkyBlock Mod
             * Dulkir
             */
        }

        return openGui
    }

    private fun renderDisplay(mods: List<Mod>) = buildList {
        for (mod in mods) {
            val currentlyOpen = mod.isInGui()
            val nameFormat = if (currentlyOpen) "§c" else ""
            var opening = mod.name == currentlyOpeningMod
            if (currentlyOpen && opening) {
                currentlyOpeningMod = ""
                opening = false
            }
            val nameSuffix = if (opening) " §7(opening...)" else ""
            val renderable = Renderable.link(
                Renderable.string(nameFormat + mod.name),
                bypassChecks = true,
                onClick = { open(mod) },
                condition = { System.currentTimeMillis() > lastGuiOpen + 250 }
            )
            add(listOf(renderable, nameSuffix))
        }
    }

    private fun open(mod: Mod) {
        lastGuiOpen = System.currentTimeMillis()
        currentlyOpeningMod = mod.name
        update()
        try {
            when (mod.command) {
                "patcher" -> {
                    println("try opening patcher")
                    // GuiUtil.open(Objects.requireNonNull(Patcher.instance.getPatcherConfig().gui()))
                    val patcher = Class.forName("club.sk1er.patcher.Patcher")
                    val instance = patcher.getDeclaredField("instance").get(null)
                    val config = instance.javaClass.getDeclaredMethod("getPatcherConfig").invoke(instance)
                    val gui = Class.forName("gg.essential.vigilance.Vigilant").getDeclaredMethod("gui").invoke(config)
                    val guiUtils = Class.forName("gg.essential.api.utils.GuiUtil")
                    for (method in guiUtils.declaredMethods) {
                        try {
                            method.invoke(null, gui)
                            println("opened patcher")
                            return
                        } catch (_: Exception) {
                        }
                    }
                    LorenzUtils.chat("§c[SkyHanni] Error trying to open the gui for mod " + mod.name + "!")
                }

                "hytil" -> {
                    println("try opening hytil")
                    // HytilsReborn.INSTANCE.getConfig().openGui()
                    val hytilsReborn = Class.forName("cc.woverflow.hytils.HytilsReborn")
                    val instance = hytilsReborn.getDeclaredField("INSTANCE").get(null)
                    val config = instance.javaClass.getDeclaredMethod("getConfig").invoke(instance)
                    val gui = Class.forName("gg.essential.vigilance.Vigilant").getDeclaredMethod("gui").invoke(config)
                    val guiUtils = Class.forName("gg.essential.api.utils.GuiUtil")
                    for (method in guiUtils.declaredMethods) {
                        try {
                            method.invoke(null, gui)
                            println("opened hytil")
                            return
                        } catch (_: Exception) {
                        }
                    }
                    LorenzUtils.chat("§c[SkyHanni] Error trying to open the gui for mod " + mod.name + "!")
                }

                else -> {
                    val thePlayer = Minecraft.getMinecraft().thePlayer
                    ClientCommandHandler.instance.executeCommand(thePlayer, "/${mod.command}")
                }
            }
        } catch (e: Exception) {
            CopyErrorCommand.logError(e, "Error trying to open the gui for mod " + mod.name)
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiScreenEvent.DrawScreenEvent.Post) {
        if (!isEnabled()) return

        GlStateManager.pushMatrix()
        config.pos.renderStringsAndItems(display, posLabel = "Quick Mod Menu Switch")
        GlStateManager.popMatrix()
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
