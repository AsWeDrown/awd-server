package gg.aswedrown.net;

public interface NetworkStatisticsHolder {

    void addIncomingTraffic(long bytes);

    void addOutgoingTraffic(long bytes);

}
