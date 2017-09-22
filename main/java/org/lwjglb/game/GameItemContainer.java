package org.lwjglb.game;

import org.joml.Vector3f;
import org.lwjglb.engine.GameItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;


public class GameItemContainer {

    private List<GameItem> solidGameItems = new ArrayList<>();
    private List<GameItemWithDistance> transparentGameItems = new ArrayList<>();


    public void addGameItem(GameItem gameItem, boolean transparent) {
        if (transparent) {
            transparentGameItems.add(new GameItemWithDistance(gameItem));
        } else {
            solidGameItems.add(gameItem);
        }
    }

    public void cleanup() {
        for (GameItem gameItem : solidGameItems) {
            gameItem.getMesh().cleanUp();
        }
        for (GameItemWithDistance gameItem : transparentGameItems) {
            gameItem.item.getMesh().cleanUp();
        }
    }

    // Fixes this bug https://www.gamedev.net/forums/topic/184383-transparency-troubles/
    // https://www.khronos.org/opengl/wiki/Transparency_Sorting Sort by closest to camera
    public void preRender(Vector3f cameraPosition) {
        for (GameItemWithDistance gameItem : transparentGameItems) {
            gameItem.distanceToCamera = cameraPosition.distance(gameItem.item.getPosition());
        }
        transparentGameItems.sort(Comparator.naturalOrder());
    }

    public void render(Consumer<GameItem> renderCallback) {
        for (GameItem gameItem : solidGameItems) {
            renderCallback.accept(gameItem);
        }
        for (GameItemWithDistance gameItem : transparentGameItems) {
            renderCallback.accept(gameItem.item);
        }
    }

    private static final class GameItemWithDistance implements Comparable<GameItemWithDistance> {
        private final GameItem item;
        private float distanceToCamera;

        private GameItemWithDistance(GameItem item) {
            this.item = item;
        }

        @Override
        public int compareTo(GameItemWithDistance o) {
            return o.distanceToCamera > distanceToCamera ? 1 : -1;
        }
    }


}
