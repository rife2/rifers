import rife.database.Datasource;
import rife.database.queries.Select;
import rife.engine.*;

public class HelloQueries extends Site {
    public void setup() {
        get("/books", c -> {
            // the driver name alone picks the SQL dialect, nothing connects
            var db = new Datasource("org.postgresql.Driver",
                "jdbc:postgresql://db/app", "app", "secret", 1);

            // build the query in type-safe Java, adding clauses conditionally
            var q = new Select(db).from("book").field("title").field("author");
            if (c.hasParameterValue("stock"))  q.where("in_stock", "=", true);
            if (c.hasParameterValue("limit"))  q.limit(5);
            if (c.hasParameterValue("offset")) q.offset(10);

            // RIFE2 renders correct SQL for that database, no strings attached
            c.print(q.getSql());
        });
    }

    public static void main(String[] args) {
        new Server().start(new HelloQueries());
    }
}
