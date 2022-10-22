package org.lwjglb.game;

import org.lwjglb.engine.GameEngine;
import org.lwjglb.engine.IGameLogic;

public class BoilerplateMain {
 
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true"); // https://stackoverflow.com/questions/74103584/creating-graphics2d-causes-glfwpollevents-to-freeze
        try {
            IGameLogic gameLogic = new DummyGame();
            GameEngine gameEng = new GameEngine("GAME", 1280, 720, gameLogic);
            gameEng.start();
        } catch (Exception excp) {
            excp.printStackTrace();
            System.exit(-1);
        }
    }
}