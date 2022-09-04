package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtTimeEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.EntityUtils.getAllNameTagsWith
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class AshfangBlazes {

    private val blazeColor = mutableMapOf<EntityBlaze, LorenzColor>()
    private val blazeArmorStand = mutableMapOf<EntityBlaze, EntityArmorStand>()

    var nearAshfang = false
    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (tick++ % 20 == 0) {
            checkNearAshfang()
        }

        if (nearAshfang) {
            for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityBlaze>()
                .filter { it !in blazeColor.keys }) {
                val list = entity.getAllNameTagsWith(2, "Ashfang")
                if (list.size == 1) {
                    val armorStand = list[0]
                    blazeArmorStand[entity] = armorStand
                    if (armorStand.name.contains("Ashfang Follower")) {
                        blazeColor[entity] = LorenzColor.DARK_GRAY
                    } else if (armorStand.name.contains("Ashfang Underling")) {
                        blazeColor[entity] = LorenzColor.RED
                    } else if (armorStand.name.contains("Ashfang Acolyte")) {
                        blazeColor[entity] = LorenzColor.BLUE
                    } else {
                        blazeArmorStand.remove(entity)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onHealthUpdatePacket(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return

        val packet = event.packet
        if (packet !is S1CPacketEntityMetadata) return
        if (packet.entityId !in blazeArmorStand.keys.map { it.entityId }) return

        for (watchableObject in packet.func_149376_c()) {
            if (watchableObject.dataValueId == 6) {
                val health = watchableObject.`object` as Float
                if (health % 10_000_000 != 0F) {
                    blazeArmorStand.keys.removeIf { it.entityId == packet.entityId }
                }
            }
        }
    }

    private fun checkNearAshfang() {
        nearAshfang = Minecraft.getMinecraft().theWorld.loadedEntityList
            .any { it is EntityArmorStand && it.name.contains("Ashfang") }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.ashfang.highlightBlazes) return
        val entity = event.entity
        event.color = blazeColor[entity]?.toColor()?.withAlpha(40) ?: 0
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtTimeEvent) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.ashfang.highlightBlazes) return
        val entity = event.entity
        if (entity in blazeColor) {
            event.shouldReset = true
        }

    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.ashfang.hideNames) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return
        if (!entity.hasCustomName()) return
        if (entity.isDead) return
        if (entity in blazeArmorStand.values) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        blazeColor.clear()
        blazeArmorStand.clear()
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
    }
}