package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class IRC extends Module {
    public static PrintWriter printWriter;
    private static final String SERVER_HOST = "47.121.113.160";
    private static final int SERVER_PORT = 6667;
    public static volatile boolean connect;
    public IRC() {
        super("IRC", Category.Client);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            return;
        }
        start();
    }

    @EventListener
    public void onupdate(UpdateEvent event) {
        if (!connect) {
            start();
        }
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof ChatMessageC2SPacket packet) {
            String s = packet.chatMessage();
            if (s.startsWith("!")) {
                try {
                    printWriter.println(s.replaceFirst("!", ""));
                } catch (Exception ignored) {
                }
                event.cancel();
            }
        }
    }

    @Override
    public void onLogin() {
        start();
    }

    @Override
    public void onDisable() {
        try {
            connect = false;
            printWriter.println("/quit");
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onLogout() {
        try {
            connect = false;
            printWriter.println("/quit");
        } catch (Exception ignored) {
        }
    }

    public void start() {
        connect = true;
        Alien.THREAD.execute(() -> {
            Scanner scanner = new Scanner(System.in);

            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Send username to server
                out.println(mc.getSession().getUsername() + "[" + Alien.userId + "]");
                IRC.printWriter = out;

                // Start a thread to read messages from server
                new Thread(() -> {
                    try {
                        String serverResponse;
                        while ((serverResponse = in.readLine()) != null) {
                            String finalServerResponse = "§b[IRC] §f" + serverResponse;
                            mc.execute(() -> mc.inGameHud.getChatHud().addMessage(Text.of(finalServerResponse)));
                        }
                    } catch (Exception e) {
                        connect = false;
                    }
                }).start();

                while (connect) {
                    Thread.onSpinWait();
                }
            } catch (UnknownHostException e) {
                connect = false;
                System.err.println("Unknown host: " + SERVER_HOST);
            } catch (IOException e) {
                connect = false;
                System.err.println("Couldn't connect to " + SERVER_HOST);
            } finally {
                connect = false;
                scanner.close();
            }
        });
    }
}
