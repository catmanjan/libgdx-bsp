package com.bsp.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.bsp.BspGame;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class DesktopLauncher {
	public static void main (String[] arg) {
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = gd.getDisplayMode().getWidth();
		config.height = gd.getDisplayMode().getHeight();
		config.fullscreen = true;
		new LwjglApplication(new BspGame(), config);
	}
}
