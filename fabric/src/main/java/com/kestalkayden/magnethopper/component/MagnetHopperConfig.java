package com.kestalkayden.magnethopper.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Data component carrying a magnet hopper's filter slots + UI toggles, preserved on break/place.
 * The filter slots themselves are wrapped in vanilla's {@link ItemContainerContents} so they
 * serialize identically to any container component.
 */
public record MagnetHopperConfig(ItemContainerContents filters, boolean whitelist, boolean magnetEnabled) {

    public static final MagnetHopperConfig DEFAULT = new MagnetHopperConfig(
        ItemContainerContents.EMPTY, true, true);

    public static final Codec<MagnetHopperConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemContainerContents.CODEC.optionalFieldOf("filters", ItemContainerContents.EMPTY).forGetter(MagnetHopperConfig::filters),
        Codec.BOOL.optionalFieldOf("whitelist", true).forGetter(MagnetHopperConfig::whitelist),
        Codec.BOOL.optionalFieldOf("magnet", true).forGetter(MagnetHopperConfig::magnetEnabled)
    ).apply(instance, MagnetHopperConfig::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagnetHopperConfig> STREAM_CODEC = StreamCodec.composite(
        ItemContainerContents.STREAM_CODEC, MagnetHopperConfig::filters,
        ByteBufCodecs.BOOL, MagnetHopperConfig::whitelist,
        ByteBufCodecs.BOOL, MagnetHopperConfig::magnetEnabled,
        MagnetHopperConfig::new);
}
