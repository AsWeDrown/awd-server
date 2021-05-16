package gg.aswedrown.game.profiling;

import gg.aswedrown.server.AwdServer;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Измеритель показателя TPS (ticks per second).
 *
 * Принцип работы:
 *
 *     1) в начале каждого цикла обновления (тика) вызывается метод onUpdate();
 *
 *     2) измеритель записывает задержку между этим тиком и предыдущим в специальную очередь;
 *
 *     3) с помощью метода estimateTps() можно в любом момент получить текущий показатель TPS^
 *        этот показатель основан на среднем арифметическом взвешенном всех задержек между тиками,
 *        в момент вызова хранящихся в специальной очереди - чем старее была сделана запись о
 *        конкретной задержке, тем меньше этот экземпляр повлияет на итоговый результат (TPS),
 *        т.е. сильнее всего на результат (TPS) влияют самые недавние показатели задержки;
 *
 *     4) метод estimateTps() может вернуть отрицательный TPS в случае, если он был вызван слишком
 *        рано (измеритель ещё не успел собрать достаточное число экземпляров для качественной оценки).
 *
 * Не рекомендуется вызывать метод estimateTps() часто - он может работать довольно медленно.
 */
public class TpsMeter {

    /**
     * За какой промежуток времени
     */
    private static final int SAMPLING_SECONDS = 60;

    /**
     * Множитель веса очередного образца после каждой итерации.
     */
    private static final float WEIGHT_MOMENTUM = 0.9995f;

    /**
     * Число наносекунд в одной секунде.
     */
    private static final float NANOS_IN_SECOND = (float) TimeUnit.SECONDS.toNanos(1);

    private final Object lock = new Object();

    private final int samplesNum;

    private final Deque<Float> recentTickDelays = new ArrayDeque<>();

    private long lastTickNanoTime;

    public TpsMeter() {
        samplesNum = SAMPLING_SECONDS * AwdServer.getServer().getConfig().getGameTps();
    }

    /**
     * Должно выполняться в начале каждого цикла обновления (тика).
     */
    public void onUpdate() {
        long thisTickNanoTime = System.nanoTime();

        if (lastTickNanoTime == 0)
            // Лениво инициализируем (чтобы не было "гигантских" значений задержки).
            lastTickNanoTime = thisTickNanoTime;

        synchronized (lock) {
            if (recentTickDelays.size() == samplesNum)
                recentTickDelays.pop(); // удаляем самый "старый" экземпляр данных

            recentTickDelays.add((float) (thisTickNanoTime - lastTickNanoTime));
            lastTickNanoTime = thisTickNanoTime;
        }
    }

    /**
     * Вычисляет текущий показатель TPS на основе хранимых в данный момент измерителем образцов.
     *
     * Возвращаемое значение будет отрицательным, если оно было вычислено приближённо, т.е. если
     * измеритель проработал недостаточно долго с момента его запуска для точного вычисления: в
     * этом случае вызвавший этот метод должен считать текущий TPS, приблизительно (!) равным
     * полученному от этого метода значению, взятому по абсолютной величине.
     */
    public float estimateTps() {
        synchronized (lock) {
            float weightedSamplesSum   = 0.0f;
            float weightsSum           = 0.0f;
            float sampleWeight         = 1.0f;

            Iterator<Float> it = recentTickDelays.descendingIterator();

            while (it.hasNext()) {
                weightedSamplesSum += sampleWeight * it.next();
                weightsSum         += sampleWeight;
                sampleWeight       *= WEIGHT_MOMENTUM;
            }

            float weightedAverageTickDelay = weightedSamplesSum / weightsSum;
            float tps = NANOS_IN_SECOND / weightedAverageTickDelay;

            return recentTickDelays.size() == samplesNum ? tps : -tps;
        }
    }

}
