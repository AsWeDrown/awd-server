package gg.aswedrown.server.data.lobby;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import gg.aswedrown.game.GameState;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.DbInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.darksidecode.kantanj.db.mongo.MongoManager;
import org.bson.Document;

import java.util.TimerTask;

@Slf4j
@RequiredArgsConstructor
public class LobbyCleaner extends TimerTask {

    private final AwdServer srv;

    private final MongoManager db;

    @Override
    public void run() {
        // Удаляем документы старее этого времени...
        long noOlderThan = System.currentTimeMillis() - srv.getConfig()
                .getDbCleanerLobbiesMaxObjectLifespanMillis();

        MongoCollection<Document> col = db
                .getCollection(DbInfo.Lobbies.COLLECTION_NAME);
        Document criteria = new Document(
                DbInfo.Lobbies.CREATION_DATE_TIME, new Document("$lte", noOlderThan))
                // ...но только те, что неактивны (комната всё ещё на предыгровом этапе).
                .append(DbInfo.Lobbies.GAME_STATE, GameState.LOBBY_STATE);

        FindIterable<Document> toDelete = col.find(criteria);
        int deletedCount = 0;

        for (Document lobbyData : toDelete) {
            // Используем именно LobbyManager для удаления, т.к. он позаботится о
            // корректном расформировании комнаты (отключении и уведомлении участников).
            deletedCount++;
            srv.getLobbyManager().deleteLobby(
                    lobbyData.getInteger(DbInfo.Lobbies.LOBBY_ID));
        }

        if (deletedCount > 0)
            log.info("DB Cleaner: deleted {} inactive lobbies.",
                    deletedCount);
    }

}
