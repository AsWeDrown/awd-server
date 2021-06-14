package gg.aswedrown.game.quest;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QuestState {

    public static final int NOT_BEGUN           = 0;

    public static final int ACTIVE              = 1;

    public static final int ENDED_FAIL          = 2;

    public static final int ENDED_COMPLETE_PART = 3;

    public static final int ENDED_COMPLETE_FULL = 4;

}
