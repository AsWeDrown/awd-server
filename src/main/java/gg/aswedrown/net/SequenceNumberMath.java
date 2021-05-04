package gg.aswedrown.net;

final class SequenceNumberMath {

    /* = 2^16-1 - 16-битное кодирование номеров пакетов (sequence number) */
    public static final int MAX_SEQUENCE_NUMBER = 65535;

    /* = 2^15-1 - половина максимума ^ (выше) */
    public static final int SEQUENCE_NUMBER_WRAP_AROUND_THRESHOLD = 32767;

    private SequenceNumberMath() {}

    static boolean isMoreRecent(int s1, int s2) {
        // Возвращаем true, если первый входной номер пакета (s1) "больше" ("новее") второго (s2).
        return ((s1 > s2) && (s1 - s2 <= SEQUENCE_NUMBER_WRAP_AROUND_THRESHOLD)) // s1 просто "новее" s2
            || ((s1 < s2) && (s2 - s1 >  SEQUENCE_NUMBER_WRAP_AROUND_THRESHOLD)); // s1 "новее" из-за wrap-around'а
    }

    static int add(int s1, int s2) {
        // Эта операция ассоциативна (входные номера пакетов МОЖНО поменять местами).
        int biggerSeq    = Math.max(s1, s2);
        int smallerSeq   = Math.min(s1, s2);
        int maxDirectAdd = MAX_SEQUENCE_NUMBER - biggerSeq;

        return smallerSeq <= maxDirectAdd
                ? biggerSeq + smallerSeq // прямое сложение (влезает в верхний предел)
                : smallerSeq - maxDirectAdd - 1; // wrap-around
    }

    static int subtract(int minuendSeq, int subtrahendSeq) {
        // Эта операция НЕ ассоциативна (входные номера пакетов НЕЛЬЗЯ поменять местами).
        return minuendSeq >= subtrahendSeq
                ? minuendSeq - subtrahendSeq // прямое вычитание (влезает в нижний предел)
                : MAX_SEQUENCE_NUMBER - (subtrahendSeq - minuendSeq) + 1; // wrap-around
    }

}
