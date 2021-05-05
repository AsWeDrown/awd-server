package gg.aswedrown.server.vircon;

import gg.aswedrown.server.AwdServer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.TimerTask;

@Slf4j
@RequiredArgsConstructor
public class Pinger extends TimerTask {

    @NonNull
    private final AwdServer srv;

    @Override
    public void run() {
        srv.getVirConManager().pingAll();
    }

}
