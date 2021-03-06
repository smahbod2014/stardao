package io.stardog.stardao.mongodb.export;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MongoImporter {
    public void importFromFile(MongoCollection<Document> collection, File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (String line; (line = reader.readLine()) != null; ) {
            Document doc = Document.parse(line);
            Object id = doc.get("_id");
            doc.remove("_id");
            collection.updateOne(new Document("_id", id), new Document("$set", doc), new UpdateOptions().upsert(true));
        }
    }
}
