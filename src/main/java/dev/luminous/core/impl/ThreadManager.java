package dev.luminous.core.impl;

import com.google.common.collect.Lists;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.JelloUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.AutoAnchor;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.render.HoleESP;
import dev.luminous.mod.modules.impl.render.PlaceRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadManager implements Wrapper {
    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    public static ClientService clientService;
    public volatile Iterable<Entity> threadSafeEntityList = Collections.emptyList();
    public volatile List<AbstractClientPlayerEntity> threadSafePlayersList = Collections.emptyList();
    public volatile boolean tickRunning = false;

    public ThreadManager() {
        init();
    }

    public void init() {
        Alien.EVENT_BUS.subscribe(this);
        clientService = new ClientService();
        clientService.setName("AlienClientService");
        clientService.setDaemon(true);
        clientService.start();

       /* try {
            String hwidTemp = System.getenv("PROCESSOR_IDENTIFIER")
                    + System.getenv("PROCESSOR_LEVEL")
                    + System.getenv("PROCESSOR_ARCHITEW6432")
                    + System.getenv("NUMBER_OF_PROCESSORS")
                    + System.getenv("COMPUTERNAME")
                    + System.getenv("PROCESSOR_REVISION")
                    + System.getProperty("user.name")
                    + System.getenv("PROCESSOR_ARCHITECTURE");

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(hwidTemp.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte aByteData : md.digest()) {
                String hex = Integer.toHexString(0xff & aByteData);
                hexString.append(hex);
            }
            String hwid = hexString.toString();

            for (String inputLine : Alien.hwidCache) {
                try {
                    String temp;
                    {
                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        SecretKeySpec secretKey;
                        {
                            byte[] key = "FUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGA".getBytes(StandardCharsets.UTF_8);
                            MessageDigest sha = MessageDigest.getInstance("SHA-256");
                            key = sha.digest(key);
                            key = Arrays.copyOf(key, 16);
                            secretKey = new SecretKeySpec(key, "AES");
                        }
                        byte[] iv = new byte[16];
                        IvParameterSpec ivParams = new IvParameterSpec(iv);
                        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
                        temp = Base64.getEncoder().encodeToString(cipher.doFinal(hwid.getBytes(StandardCharsets.UTF_8)));
                    }
                    String temp2;
                    {
                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        SecretKeySpec secretKey;
                        {
                            byte[] key = "Ttwu0jVhihBQhobbLIkAeaAWKpgydqpkhMnyiDrorXDoe2N1Mt".getBytes(StandardCharsets.UTF_8);
                            MessageDigest sha = MessageDigest.getInstance("SHA-256");
                            key = sha.digest(key);
                            key = Arrays.copyOf(key, 16);
                            secretKey = new SecretKeySpec(key, "AES");
                        }
                        byte[] iv = new byte[16];
                        IvParameterSpec ivParams = new IvParameterSpec(iv);
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
                        byte[] original = cipher.doFinal(Base64.getDecoder().decode(inputLine));
                        temp2 = new String(original, StandardCharsets.UTF_8);
                    }
                    if (temp.equals(temp2)) {
                        try {
                            {
                                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                SecretKeySpec secretKey;
                                {
                                    byte[] key = "FUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGA".getBytes(StandardCharsets.UTF_8);
                                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                                    key = sha.digest(key);
                                    key = Arrays.copyOf(key, 16);
                                    secretKey = new SecretKeySpec(key, "AES");
                                }
                                byte[] iv = new byte[16];
                                IvParameterSpec ivParams = new IvParameterSpec(iv);
                                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
                                temp = Base64.getEncoder().encodeToString(cipher.doFinal(hwid.getBytes(StandardCharsets.UTF_8)));
                            }
                            {
                                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                SecretKeySpec secretKey;
                                {
                                    byte[] key = "Ttwu0jVhihBQhobbLIkAeaAWKpgydqpkhMnyiDrorXDoe2N1Mt".getBytes(StandardCharsets.UTF_8);
                                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                                    key = sha.digest(key);
                                    key = Arrays.copyOf(key, 16);
                                    secretKey = new SecretKeySpec(key, "AES");
                                }
                                byte[] iv = new byte[16];
                                IvParameterSpec ivParams = new IvParameterSpec(iv);
                                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
                                byte[] original = cipher.doFinal(Base64.getDecoder().decode(inputLine));
                                temp2 = new String(original, StandardCharsets.UTF_8);
                            }
                            if (!temp.equals(temp2)) {
                                try {
                                    MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                                                    "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                                            .invoke(0, 0, 0, 0);
                                } catch (Throwable ignored) {
                                }
                                MinecraftClient.getInstance().close();
                                System.gc();
                                System.exit(-1);
                            }
                            {
                                String hwidTemp2 = System.getenv("PROCESSOR_IDENTIFIER")
                                        + System.getenv("PROCESSOR_LEVEL")
                                        + System.getenv("PROCESSOR_ARCHITEW6432")
                                        + System.getenv("NUMBER_OF_PROCESSORS")
                                        + System.getenv("COMPUTERNAME")
                                        + System.getenv("PROCESSOR_REVISION")
                                        + System.getProperty("user.name")
                                        + System.getenv("PROCESSOR_ARCHITECTURE");
                                MessageDigest md2 = MessageDigest.getInstance("MD5");
                                md2.update(hwidTemp2.getBytes());
                                StringBuilder hexString2 = new StringBuilder();
                                for (byte aByteData : md2.digest()) {
                                    String hex = Integer.toHexString(0xff & aByteData);
                                    hexString2.append(hex);
                                }
                                String hwid2 = hexString2.toString();
                                if (!hwid2.equals(hwid)) {
                                    try {

                                        MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                                                        "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                                                .invoke(0, 0, 0, 0);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                            Alien.EVENT_BUS.subscribe(this);
                            clientService = new ClientService();
                            clientService.setName("AlienClientService");
                            clientService.setDaemon(true);
                            clientService.start();
                            if (hwid.isEmpty()) {
                                try {
                                    MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                                                    "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                                            .invoke(0, 0, 0, 0);
                                } catch (Throwable ignored) {
                                }
                                MinecraftClient.getInstance().close();
                                System.gc();
                                System.exit(-1);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                            "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                    .invoke(0, 0, 0, 0);
        } catch (Throwable ignored) {
        }
        MinecraftClient.getInstance().close();
        System.gc();
        System.exit(-1);*/
    }

    public Iterable<Entity> getEntities() {
        return threadSafeEntityList;
    }

    public List<AbstractClientPlayerEntity> getPlayers() {
        return threadSafePlayersList;
    }

    public void execute(Runnable runnable) {
        EXECUTOR.execute(runnable);
    }

    @EventListener(priority = EventPriority.HIGHEST)
    public void onEvent(ClientTickEvent event) {
        Alien.POP.onUpdate();
        Alien.SERVER.onUpdate();
        if (event.isPre()) {
            JelloUtil.updateJello();
            tickRunning = true;
            BlockUtil.placedPos.forEach(pos -> PlaceRender.INSTANCE.create(pos));
            BlockUtil.placedPos.clear();
            Alien.PLAYER.onUpdate();
            if (!Module.nullCheck())
                Alien.EVENT_BUS.post(UpdateEvent.INSTANCE);
        } else {
            tickRunning = false;
            if (mc.world == null || mc.player == null) return;
            threadSafeEntityList = Lists.newArrayList(mc.world.getEntities());
            threadSafePlayersList = Lists.newArrayList(mc.world.getPlayers());
        }

        if (!clientService.isAlive() || clientService.isInterrupted()) {
            clientService = new ClientService();
            clientService.setName("AlienService");
            clientService.setDaemon(true);
            clientService.start();
        }
    }

    public class ClientService extends Thread {
        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            while (true) {
                try {
                    while (tickRunning) {
                        Thread.onSpinWait();
                    }
                    AutoCrystal.INSTANCE.onThread();
                    HoleESP.INSTANCE.onThread();
                    AutoAnchor.INSTANCE.onThread();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (ClientSetting.INSTANCE.debug.getValue())
                        CommandManager.sendMessage("§4An error has occurred [Thread] Message: [" + e.getMessage() + "]");
                }
            }
        }
    }
}