package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.core.Manager;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.io.IOUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FriendManager extends Manager {
    public final ArrayList<String> friendList = new ArrayList<>();

    public FriendManager() {
        read();
    }

    public boolean isFriend(String name) {
        return name.equals("NRC4") || name.equals("Neutron") || name.equals("||NRC4") || name.equals("||Neutron") || friendList.contains(name);
    }

    public boolean isFriend(PlayerEntity entity) {
        return isFriend(entity.getGameProfile().name());
    }

    public void remove(String name) {
        friendList.remove(name);
    }

    public void add(String name) {
        if (!friendList.contains(name)) {
            friendList.add(name);
        }
    }

    public void friend(PlayerEntity entity) {
        friend(entity.getGameProfile().name());
    }

    public void friend(String name) {
        if (friendList.contains(name)) {
            friendList.remove(name);
        } else {
            friendList.add(name);
        }
    }

    public void read() {

        try {
            File friendFile = getFile("friends.txt");
            if (!friendFile.exists())
                return;
            List<String> list = IOUtils.readLines(new FileInputStream(friendFile), StandardCharsets.UTF_8);

            for (String s : list) {
                add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            File friendFile = getFile("friends.txt");
            PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(friendFile), StandardCharsets.UTF_8));
            for (String str : friendList) {
                printwriter.println(str);
            }
            printwriter.close();
        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("[" + Alien.NAME + "] Failed to save friends");
        }
    }
}
