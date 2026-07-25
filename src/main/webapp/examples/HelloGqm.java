package com.example;

import rife.database.*;
import rife.database.querymanagers.generic.*;
import rife.engine.*;

public class HelloGqm extends Site {
    Datasource datasource = new Datasource(
        "org.h2.Driver", "jdbc:h2:mem:hello", "sa", "", 5);

    // derives the table and all the queries straight from the Book bean
    GenericQueryManager<Book> books =
        GenericQueryManagerFactory.instance(datasource, Book.class);

    public void setup() {
        get("/install", c -> { books.install(); c.print("ready"); });

        // save a bean, its generated id comes straight back
        post("/books", c -> {
            var book = c.parametersBean(Book.class);
            c.print("saved as #" + books.save(book));
        });

        // restore every stored bean, ordered, with no row mapping
        get("/books", c -> books.restore(books.getRestoreQuery().orderBy("title"))
            .forEach(b ->
                c.print(b.getTitle() + " by " + b.getAuthor() + "<br>")));
    }

    public static void main(String[] args) {
        new Server().start(new HelloGqm());
    }
}

// the table and its queries are derived straight from this plain bean
class Book {
    private int id = -1;
    private String title;
    private String author;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
}
