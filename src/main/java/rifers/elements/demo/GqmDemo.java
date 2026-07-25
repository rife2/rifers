package rifers.elements.demo;

import rife.database.Datasource;
import rife.database.querymanagers.generic.GenericQueryManagerFactory;
import rife.engine.Context;
import rife.engine.Element;
import rife.engine.annotations.Parameter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A Generic Query Manager demo that stays completely stateless. Every request
 * spins up a private in-memory H2, lets the manager create the table straight
 * from the {@link Book} bean, saves a couple of seed rows plus whatever the
 * visitor added, then restores and counts them through the same manager and
 * destroys the database again before responding.
 * <p>
 * There is no SQL and no row mapping here: the manager derives the schema and
 * the reads and writes from the bean itself.
 */
public class GqmDemo implements Element {
    static final AtomicLong SEQUENCE = new AtomicLong();

    /**
     * A plain bean; the manager turns it into a table and back with no
     * annotations or mapping code.
     */
    public static class Book {
        private Integer id_;
        private String title_;
        private String author_;

        public void setId(Integer id)         { id_ = id; }
        public Integer getId()                { return id_; }
        public void setTitle(String title)    { title_ = title; }
        public String getTitle()              { return title_; }
        public void setAuthor(String author)  { author_ = author; }
        public String getAuthor()             { return author_; }
    }

    @Parameter String title = "";
    @Parameter String author = "";

    public void process(Context c) {
        var datasource = new Datasource("org.h2.Driver", "jdbc:h2:mem:demogqm" + SEQUENCE.incrementAndGet(), "sa", "", 5);
        // hold one connection open so the in-memory database stays alive for the
        // request; closing it and cleaning up the pool destroys the database
        var keepAlive = datasource.getConnection();
        try {
            var manager = GenericQueryManagerFactory.instance(datasource, Book.class);
            manager.install();
            // seed a couple of rows so the list is never empty
            manager.save(book("The Pragmatic Programmer", "Hunt and Thomas"));
            manager.save(book("Effective Java", "Joshua Bloch"));

            Integer newId = null;
            if (!title.isBlank()) {
                newId = manager.save(book(title.trim(), author.isBlank() ? "Unknown" : author.trim()));
            }

            var books = manager.restore(manager.getRestoreQuery().orderBy("id"));
            var count = manager.count();

            var t = c.template("demo/gqm");
            t.setValue("run_url", c.urlFor(c.route()).toString());
            t.setValueEncoded("title", title);
            t.setValueEncoded("author", author);
            for (var b : books) {
                t.setValue("book_id", b.getId());
                t.setValueEncoded("book_title", b.getTitle());
                t.setValueEncoded("book_author", b.getAuthor());
                t.appendBlock("rows", "book_row");
            }
            t.setValue("count", count);
            if (newId != null) {
                t.setValue("new_id", newId);
                t.setValueEncoded("new_title", title.trim());
                t.setBlock("saved", "saved_note");
                // fire a client-side event alongside the swap: the record is
                // serialized to JSON by RIFE2 and delivered as the event detail,
                // so site.js can flash a toast without any extra request
                c.hxTrigger("bookSaved", new Saved(newId, title.trim()));
            }
            c.printHtmxFragment(t, "stage");
        } finally {
            keepAlive.close();
            datasource.cleanup();
        }
    }

    // the payload delivered to the client with the HX-Trigger event; its
    // components become the JSON keys automatically, no annotations needed
    record Saved(int id, String title) {}

    private static Book book(String title, String author) {
        var b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        return b;
    }
}
