package io.stardog.stardao;

import com.github.fakemongo.Fongo;
import com.google.common.collect.ImmutableSet;
import io.stardog.stardao.core.Update;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AbstractMongoDaoTest {
    private TestUserDao dao;

    @Before
    public void setUp() throws Exception {
        Fongo fongo = new Fongo("fake-mongo");
        dao = new TestUserDao(fongo.getMongo().getDatabase("test-mongo").getCollection("test-user"));

    }

    @Test
    public void testGetCollection() throws Exception {
        assertEquals("test-user", dao.getCollection().getNamespace().getCollectionName());
    }

    @Test
    public void testGenerateId() throws Exception {
        assertTrue(dao.generateId() instanceof ObjectId);
    }

    @Test
    public void testCreateAndLoadOpt() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").build());

        TestUser load = dao.loadOpt(created.getId()).get();
        assertEquals(load, created);
    }

    @Test
    public void testCreate() throws Exception {
        Instant now = Instant.now();
        ObjectId creatorId = new ObjectId();
        TestUser created = dao.create(TestUser.builder().name("Ian").build(), now, creatorId);
        assertEquals(creatorId, created.getCreateId());
        assertEquals(now, created.getCreateAt());
    }

    @Test
    public void testUpdate() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").email("ian@example.com").build());

        ObjectId updateBy = new ObjectId();
        Instant now = Instant.now();

        Update<TestUser> update = Update.of(
                TestUser.builder().name("Bob").build(),
                ImmutableSet.of("name"),
                ImmutableSet.of("email"));
        dao.update(created.getId(), update, now, updateBy);

        TestUser load = dao.load(created.getId());
        assertEquals("Bob", load.getName());
        assertEquals(updateBy, load.getUpdateId());
        assertEquals(now, load.getUpdateAt());
        assertNull(load.getEmail());
    }

    @Test
    public void testUpdateAndReturn() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").build());

        ObjectId updateBy = new ObjectId();
        Instant now = Instant.now();

        Update<TestUser> update = Update.of(
                TestUser.builder().name("Bob").build(),
                ImmutableSet.of("name"));
        TestUser prev = dao.updateAndReturn(created.getId(), update, now, updateBy);

        assertEquals(prev, created);

        TestUser load = dao.load(created.getId());
        assertEquals("Bob", load.getName());
        assertEquals(updateBy, load.getUpdateId());
        assertEquals(now, load.getUpdateAt());
    }

    @Test
    public void testToUpdateDocument() throws Exception {
        Update<TestUser> update = Update.of(
                TestUser.builder().birthday(LocalDate.of(1980, 5, 12)).build(),
                ImmutableSet.of("birthday"),
                ImmutableSet.of("email"));
        Instant now = Instant.now();
        ObjectId updater = new ObjectId();
        Document doc = dao.toUpdateDocument(update, now, updater);
        Document expected = new Document("$set",
                new Document("birthday", "1980-05-12")
                    .append("updateAt", Date.from(now))
                    .append("updateId", updater))
                .append("$unset", new Document("email", 1));
        assertEquals(doc, expected);
    }

    @Test
    public void testDelete() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").build());

        dao.delete(created.getId());

        Optional<TestUser> load = dao.loadOpt(created.getId());
        assertFalse(load.isPresent());
    }

    @Test
    public void testIterateAll() throws Exception {

    }

    @Test
    public void testInitTable() throws Exception {

    }

    @Test
    public void testDropTable() throws Exception {

    }

    @Test
    public void testGetIndexes() throws Exception {

    }
}