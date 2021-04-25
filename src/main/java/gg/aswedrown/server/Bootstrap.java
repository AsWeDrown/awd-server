package gg.aswedrown.server;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bootstrap {

    public static void main(String[] args) {
        try {
            AwdServer.getServer().bootstrap();
        } catch (Exception ex) {
            log.error("Failed to start AwdServer (fatal Bootstrap error)!", ex);
            log.error("Exiting in 30 seconds...");

            try {
                Thread.sleep(30000);
            } catch (InterruptedException ignored) {}

            log.error("Exiting with exit code 1.");
            System.exit(1);
        }
    }

}
