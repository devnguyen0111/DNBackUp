package org.dnplugins.dnbackup.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BackupProgressPayload(
    float progress, 
    String status, 
    int processedFiles, 
    int totalFiles, 
    long totalSize
) implements CustomPacketPayload {
    public static final Type<BackupProgressPayload> TYPE = 
        new Type<>(Identifier.fromNamespaceAndPath("dnbackup", "progress"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, BackupProgressPayload> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeFloat(packet.progress());
            buf.writeUtf(packet.status());
            buf.writeInt(packet.processedFiles());
            buf.writeInt(packet.totalFiles());
            buf.writeLong(packet.totalSize());
        },
        buf -> new BackupProgressPayload(
            buf.readFloat(), 
            buf.readUtf(), 
            buf.readInt(), 
            buf.readInt(), 
            buf.readLong()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
