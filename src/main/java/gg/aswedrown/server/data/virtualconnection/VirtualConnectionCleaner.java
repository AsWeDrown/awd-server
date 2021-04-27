package gg.aswedrown.server.data.virtualconnection;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
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

        DeleteResult result = col.deleteMany(criteria);

        if (result.wasAcknowledged() && result.getDeletedCount() > 0)
            log.info("DB Cleaner: deleted {} inactive virtual connections.",
                    result.getDeletedCount());
    }

}
