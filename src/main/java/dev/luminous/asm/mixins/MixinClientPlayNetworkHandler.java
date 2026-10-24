package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.*;
import dev.luminous.mod.modules.impl.exploit.AntiPacket;
import dev.luminous.mod.modules.impl.exploit.Blink;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPosition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;

@Mixin(value={ClientPlayNetworkHandler.class})
public abstract class MixinClientPlayNetworkHandler
        extends ClientCommonNetworkHandler {
    @Shadow
    private ClientWorld world;
    @Unique
    private boolean alien$worldNotNull;
    @Unique
    private boolean ignore;
    @Unique
    private boolean alien$ignoreExplosion;

    protected MixinClientPlayNetworkHandler(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method={"onEnterReconfiguration"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift=At.Shift.AFTER)})
    private void onEnterReconfiguration(EnterReconfigurationS2CPacket packet, CallbackInfo info) {
        Alien.EVENT_BUS.post(GameLeftEvent.INSTANCE);
    }

    @Inject(method={"onGameJoin"}, at={@At(value="HEAD")})
    private void onGameJoinHead(GameJoinS2CPacket packet, CallbackInfo info) {
        this.alien$worldNotNull = this.world != null;
    }

    @Inject(method={"onGameJoin"}, at={@At(value="TAIL")})
    private void onGameJoinTail(GameJoinS2CPacket packet, CallbackInfo info) {
        if (this.alien$worldNotNull) {
            Alien.EVENT_BUS.post(GameLeftEvent.INSTANCE);
        }
    }

    @Shadow
    public abstract void sendChatMessage(String var1);

    @Inject(method={"onInventory"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift=At.Shift.AFTER)}, cancellable=true)
    public void onInventoryS2CPacket(InventoryS2CPacket packet, CallbackInfo ci) {
        InventoryS2CPacketEvent event = InventoryS2CPacketEvent.get(packet);
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method={"sendChatMessage"}, at={@At(value="HEAD")}, cancellable=true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (this.ignore) {
            return;
        }
        if (message.startsWith(Alien.getPrefix())) {
            Alien.COMMAND.command(message.split(" "));
            ci.cancel();
        } else {
            SendMessageEvent event = SendMessageEvent.get(message);
            Alien.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                ci.cancel();
            } else if (!event.message.equals(event.defaultMessage)) {
                this.ignore = true;
                this.sendChatMessage(event.message);
                this.ignore = false;
                ci.cancel();
            }
        }
    }

    @Inject(method={"onCloseScreen"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift=At.Shift.AFTER)}, cancellable=true)
    public void onCloseScreen(CloseScreenS2CPacket packet, CallbackInfo ci) {
        S2CCloseScreenEvent event = S2CCloseScreenEvent.get();
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Redirect(method={"onEntityVelocityUpdate"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/Entity;setVelocityClient(Lnet/minecraft/util/math/Vec3d;)V"), require=0)
    private void velocityHook(Entity instance, Vec3d velocity) {
        EntityVelocityUpdateEvent event = EntityVelocityUpdateEvent.get(instance, velocity.x, velocity.y, velocity.z, false);
        Alien.EVENT_BUS.post(event);
        if (!event.isCancelled()) {
            instance.setVelocityClient(new Vec3d(event.getX(), event.getY(), event.getZ()));
        }
    }

    @Inject(method = "onExplosion", at = @At("HEAD"))
    private void velocityHook2(ExplosionS2CPacket packet, CallbackInfo ci) {
        packet.playerKnockback().ifPresent(vec3d -> {
            EntityVelocityUpdateEvent event = EntityVelocityUpdateEvent.get(MinecraftClient.getInstance().player, vec3d.getX(), vec3d.getY(), vec3d.getZ(), true);
            Alien.EVENT_BUS.post(event);
            if (!event.isCancelled()) {
                MinecraftClient.getInstance().player.addVelocityInternal(new Vec3d(event.getX(), event.getY(), event.getZ()));
            }
        });
    }


    @Inject(method={"onExplosion"}, at={@At(value="HEAD")}, cancellable=true)
    private void onExplosionHead(ExplosionS2CPacket packet, CallbackInfo ci) {
        if (this.alien$ignoreExplosion || !Blink.INSTANCE.isOn() || !Blink.INSTANCE.velocity.getValue() || packet.playerKnockback().isEmpty()) {
            return;
        }
        ci.cancel();
        ExplosionS2CPacket cleanPacket = new ExplosionS2CPacket(packet.center(), packet.radius(), packet.blockCount(), Optional.empty(), packet.explosionParticle(), packet.explosionSound(), packet.blockParticles());
        this.alien$ignoreExplosion = true;
        ((ClientPlayNetworkHandler)(Object)this).onExplosion(cleanPacket);
        this.alien$ignoreExplosion = false;
    }

    @Inject(method={"onPlayerPositionLook"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift=At.Shift.AFTER)}, cancellable=true)
    public void onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        boolean noRotate;
        boolean bl = noRotate = AntiPacket.INSTANCE.isOn() && AntiPacket.INSTANCE.s2CRotate.getValue() && Alien.SERVER.playerNull.passedS(0.25);
        if (noRotate) {
            double i;
            double h;
            double g;
            double f;
            double e;
            double d;
            ci.cancel();
            NetworkThreadUtils.forceMainThread(packet, (ClientPlayPacketListener)(Object)this, this.client.getPacketApplyBatcher());
            ClientPlayerEntity playerEntity = this.client.player;
            Vec3d vec3d = playerEntity.getVelocity();
            Vec3d changePos = packet.change().position();
            boolean bl2 = packet.relatives().contains(PositionFlag.X);
            boolean bl22 = packet.relatives().contains(PositionFlag.Y);
            boolean bl3 = packet.relatives().contains(PositionFlag.Z);
            if (bl2) {
                d = vec3d.getX();
                e = playerEntity.getX() + changePos.x;
                playerEntity.lastRenderX += changePos.x;
                playerEntity.lastRenderX += changePos.x;
            } else {
                d = 0.0;
                playerEntity.lastRenderX = e = changePos.x;
                playerEntity.lastRenderX = e;
            }
            if (bl22) {
                f = vec3d.getY();
                g = playerEntity.getY() + changePos.y;
                playerEntity.lastRenderY += changePos.y;
                playerEntity.lastRenderY += changePos.y;
            } else {
                f = 0.0;
                playerEntity.lastRenderY = g = changePos.y;
                playerEntity.lastRenderY = g;
            }
            if (bl3) {
                h = vec3d.getZ();
                i = playerEntity.getZ() + changePos.z;
                playerEntity.lastRenderZ += changePos.z;
                playerEntity.lastRenderZ += changePos.z;
            } else {
                h = 0.0;
                playerEntity.lastRenderZ = i = changePos.z;
                playerEntity.lastRenderZ = i;
            }
            playerEntity.setPosition(e, g, i);
            playerEntity.setVelocity(d, f, h);
            if (AntiPacket.INSTANCE.applyYaw.getValue()) {
                float yaw = packet.change().yaw();
                float pitch = packet.change().pitch();
                if (packet.relatives().contains(PositionFlag.X_ROT)) {
                    pitch += Alien.ROTATION.getLastPitch();
                }
                if (packet.relatives().contains(PositionFlag.Y_ROT)) {
                    yaw += Alien.ROTATION.getLastYaw();
                }
                this.connection.send((Packet)new TeleportConfirmC2SPacket(packet.teleportId()));
                this.connection.send((Packet)new PlayerMoveC2SPacket.Full(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), yaw, pitch, false, false));
            } else {
                this.connection.send((Packet)new TeleportConfirmC2SPacket(packet.teleportId()));
                this.connection.send((Packet)new PlayerMoveC2SPacket.Full(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch(), false, false));
            }
            Alien.EVENT_BUS.post(ServerChangePositionEvent.INSTANCE);
        }
    }
}

