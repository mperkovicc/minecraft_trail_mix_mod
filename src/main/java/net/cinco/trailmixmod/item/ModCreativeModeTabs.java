package net.cinco.trailmixmod.item;

import net.cinco.trailmixmod.TrailMixMod;
import net.cinco.trailmixmod.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TrailMixMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> TRAIL_TAB = CREATIVE_MODE_TABS.register("trail_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(Items.SUGAR))
                    .title(Component.translatable("creativetab.trail_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(Items.REDSTONE);
                        pOutput.accept(Items.GLOWSTONE_DUST);
                        pOutput.accept(Items.SUGAR);
                        pOutput.accept(Items.GUNPOWDER);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
