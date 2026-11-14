package satin;

import javax.swing.*;

/** Optional startup notice retained for the user's explicit server message. */
public final class Checks {
   private Checks() {
   }

   public static void warn() {
      show("交流群：1079451026");
   }

   public static void check() {
   }

   private static void show(String message) {
      try {
         JFrame frame = new JFrame();
         frame.setAlwaysOnTop(true);
         JOptionPane.showMessageDialog(frame, message, "Hachimi", JOptionPane.INFORMATION_MESSAGE, UIManager.getIcon("OptionPane.informationIcon"));
         frame.dispose();
      } catch (Throwable ignored) {
         // Headless/server environments cannot show Swing windows.
      }
   }
}
