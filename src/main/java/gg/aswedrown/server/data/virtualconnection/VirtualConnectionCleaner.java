package gg.aswedrown.server.data.virtualconnection;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.DbInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.darksidecode.kantanj.db.mongo.MongoManager;
import org.bson.Document;

import java.util.TimerTask;

@Slf4j
@RequiredArgsConstructor
public class VirtualConnectionCleaner extends TimerTask {

    private final AwdServer srv;

    private final MongoManager db;

    @Override
    public void run() {
        // Удаляем документы старее этого времени.
        long noOlderThan = System.currentTimeMillis() - srv.getConfig()
                .getDbCleanerVirtualConnectionsMaxObjectLifespanMillis();

        MongoCollection<Document> col = db
                .getCollection(DbInfo.VirtualConnections.COLLECTION_NAME);
        Document criteria = new Document(
                DbInfo.VirtualConnections.LAST_PACKET_RECEIVED_DATE_TIME,
                new Document("$lte", noOlderThan));

        FindIterable<Document> toDelete = col.find(criteria);
        int deletedCount = 0;

        for (Document virConData : toDelete) {
            // Используем именно VirtualConnectionManager для удаления, т.к. он позаботится о
            // корректном "отключении" клиента (удалении его данных из памяти, например, Пингера).
            deletedCount++;
            srv.getVirConManager().closeVirtualConnection(
                    virConData.getString(DbInfo.VirtualConnections.ADDR_STR));
        }

        if (deletedCount > 0)
            log.info("DB Cleaner: closed {} inactive virtual connections.",
                    deletedCount);
    }

}
