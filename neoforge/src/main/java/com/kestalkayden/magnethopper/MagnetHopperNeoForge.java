package com.kestalkayden.magnethopper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kestalkayden.magnethopper.block.MagnetHopperBlockEntities;
import com.kestalkayden.magnethopper.block.MagnetHopperBlocks;
import com.kestalkayden.magnethopper.client.MagnetHopperScreen;
import com.kestalkayden.magnethopper.component.MagnetHopperComponents;
import com.kestalkayden.magnethopper.menu.MagnetHopperMenus;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

@Mod(MagnetHopperNeoForge.MOD_ID)
public class MagnetHopperNeoForge {
    public static final String MOD_ID = "magnethopper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public MagnetHopperNeoForge(IEventBus modBus) {
        LOGGER.info("Initializing Magnet Hopper (NeoForge)");

        MagnetHopperBlocks.BLOCKS.register(modBus);
        MagnetHopperBlocks.ITEMS.register(modBus);
        MagnetHopperBlockEntities.BES.register(modBus);
        MagnetHopperMenus.MENUS.register(modBus);
        MagnetHopperComponents.COMPONENTS.register(modBus);

        // Bind loader-agnostic refs (DeferredHolders resolve lazily, after registration fires).
        MagnetHopperRefs.MAGNET_HOPPER_BE = MagnetHopperBlockEntities.MAGNET_HOPPER_BE_HOLDER::get;
        MagnetHopperRefs.MAGNET_HOPPER_MENU = MagnetHopperMenus.MAGNET_HOPPER_MENU_HOLDER::get;
        MagnetHopperRefs.CONFIG = MagnetHopperComponents::CONFIG;

        modBus.addListener(MagnetHopperNeoForge::onBuildCreativeTabs);
        modBus.addListener(MagnetHopperNeoForge::onRegisterCapabilities);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modBus.addListener(MagnetHopperNeoForge::onRegisterMenuScreens);
        }
    }

    /** Expose our inventory to pipe mods (Pipez, EnderIO, AE2, etc.) via NeoForge's Item
     *  block capability. WorldlyContainerWrapper respects WorldlyContainer (our filter), so
     *  pipes can't push/pull blacklisted items in either direction. */
    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            MagnetHopperBlockEntities.MAGNET_HOPPER_BE,
            (be, side) -> side != null
                ? new WorldlyContainerWrapper(be, side)
                : VanillaContainerWrapper.of(be));
    }

    private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            ItemStack anchor = new ItemStack(Items.HOPPER);
            for (var holder : new java.util.function.Supplier[] {
                    () -> MagnetHopperBlocks.MAGNET_HOPPER_ITEM.get(),
                    () -> MagnetHopperBlocks.ADVANCED_MAGNET_HOPPER_ITEM.get(),
                    () -> MagnetHopperBlocks.INDUSTRIAL_MAGNET_HOPPER_ITEM.get() }) {
                ItemStack stack = new ItemStack((net.minecraft.world.item.Item) holder.get());
                event.insertAfter(anchor, stack,
                    net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                anchor = stack;
            }
        }
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MagnetHopperMenus.MAGNET_HOPPER_MENU, MagnetHopperScreen::new);
    }
}
