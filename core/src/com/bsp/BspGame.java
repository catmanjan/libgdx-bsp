package com.bsp;

import com.badlogic.gdx.Game;

public class BspGame extends Game {
	@Override
	public void create() {
		setScreen(new GameScreen());
	}
}
