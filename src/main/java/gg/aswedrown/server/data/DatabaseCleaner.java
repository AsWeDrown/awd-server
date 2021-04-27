package gg.aswedrown.server.data;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.darksidecode.kantanj.db.mongo.MongoManager;
import org.bson.Document;

import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Автоматически удаляет из базы данных, точнее, из коллекции `observedCollection`,
 * все объекты, с момента последней "активности" которых (этот момент задаётся в каждой
 * коллекции своим полем - `triggerTimestampKey`) прошло не менее maxLifespanMillis
 * миллисекунд и которые удовлетворяют критерию, который добавляет `extraCriteriaAppender`.
 * Если `extraCriteriaAppender` не указан (null), то проверяется лишь "время жизни" объектов.
 */
@Slf4j
@RequiredArgsConstructor
public class DatabaseCleaner extends TimerTask {

    @NonNull
    private final MongoManager db;

    @NonNull
    private final String observedCollection, triggerTimestampKey;

    private final long maxLifespanMillis;

    private final Consumer<Document> extraCriteriaAppender;

    @Override
    public void run() {
        long minAllowedTimestamp = System.currentTimeMillis() - maxLifespanMillis;

        MongoCollection<Document> col = db.getCollection(observedCollection);
        Document criteria = new Document(triggerTimestampKey, new Document("$lte", minAllowedTimestamp));

        if (extraCriteriaAppender != null)
            extraCriteriaAppender.accept(criteria);

        DeleteResult result = col.deleteMany(criteria);

        if (result.getDeletedCount() > 0)
            log.info("Cleaned MongoDB collection \"{}\". {} old object(s) deleted.",
                    observedCollection, result.getDeletedCount());
    }

}
