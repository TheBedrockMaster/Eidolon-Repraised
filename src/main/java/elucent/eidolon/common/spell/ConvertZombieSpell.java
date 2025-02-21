package elucent.eidolon.common.spell;

import elucent.eidolon.api.altar.AltarInfo;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.capability.ISoul;
import elucent.eidolon.common.deity.Deities;
import elucent.eidolon.common.deity.DeityLocks;
import elucent.eidolon.common.tile.EffigyTileEntity;
import elucent.eidolon.registries.Registry;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ConvertZombieSpell extends PrayerSpell {
    public ConvertZombieSpell(ResourceLocation resourceLocation, int baseRep, double powerMult, Sign... signs) {
        super(resourceLocation, Deities.LIGHT_DEITY, 20, baseRep, powerMult, signs);
    }

    @Override
    public boolean canCast(Level world, BlockPos pos, Player player) {
        HitResult ray = rayTrace(player, player.getBlockReach(), 0, true);
        boolean flag = ray instanceof EntityHitResult result && result.getEntity() instanceof ZombieVillager;
        EffigyTileEntity effigy = getEffigy(world, pos);
        if (effigy == null) {
            player.displayClientMessage(Component.translatable("eidolon.message.no_effigy"), true);
            return false;
        }
        AltarInfo info = AltarInfo.getAltarInfo(world, effigy.getBlockPos());
        if (info.getAltar() != Registry.STONE_ALTAR.get() || info.getIcon() != Registry.ELDER_EFFIGY.get())
            return false;
        return flag && super.canCast(world, pos, player);
    }

    @Override
    public void cast(Level world, BlockPos pos, Player player) {
        EffigyTileEntity effigy = getEffigy(world, pos);
        if (effigy == null) return;

        HitResult ray = rayTrace(player, player.getBlockReach(), 0, true);
        if (!(ray instanceof EntityHitResult result && result.getEntity() instanceof ZombieVillager villager)) return;

        if (world instanceof ServerLevel) {
            effigy.pray();
            AltarInfo info = AltarInfo.getAltarInfo(world, effigy.getBlockPos());
            world.getCapability(IReputation.INSTANCE, null).ifPresent((rep) -> {
                rep.pray(player, this, world.getGameTime());
                KnowledgeUtil.grantResearchNoToast(player, DeityLocks.CURE_ZOMBIE);
                rep.addReputation(player, deity.getId(), getBaseRep() + getPowerMultiplier() * info.getPower());
                updateMagic(info, player, world, rep.getReputation(player, deity.getId()));
            });
            villager.startConverting(player.getUUID(), 20);
            ISoul.expendMana(player, getCost());
        } else {
            playSuccessSound(world, player, effigy, Signs.HARMONY_SIGN);
        }
    }
}
