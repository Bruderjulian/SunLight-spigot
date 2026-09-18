package su.nightexpress.sunlight.module.scoreboard.board;

import org.jetbrains.annotations.NotNull;

public interface Board {

    BoardDefinition getBoardConfig();

    void create();

    void update();

    void updateIfReady();

    void remove();
}
