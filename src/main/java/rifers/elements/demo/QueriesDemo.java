package rifers.elements.demo;

import rife.database.Datasource;
import rife.database.exceptions.UnsupportedSqlFeatureException;
import rife.database.queries.Select;
import rife.engine.Context;
import rife.engine.Element;

/**
 * A query-builder demo. A query is built in plain, type-safe Java by toggling
 * a few clauses on and off, and RIFE2 renders it as SQL in the dialect of
 * whichever database is chosen. The same Java produces genuinely different SQL
 * per database: MySQL writes a boolean as 1, HSQLDB puts LIMIT right after
 * SELECT, and Oracle has no LIMIT or OFFSET at all. The dialect comes from the
 * driver name alone, so no JDBC drivers are on the classpath and nothing ever
 * connects.
 */
public class QueriesDemo implements Element {
    private static final String[][] DATABASES = {
        {"postgresql", "PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://db/app"},
        {"mysql", "MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://db/app"},
        {"hsqldb", "HSQLDB", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:app"},
        {"oracle", "Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@db:1521:app"},
    };

    public void process(Context c) {
        var db = database(c.parameter("db", "postgresql"));
        var ds = new Datasource(db[2], db[3], "app", "secret", 1);

        var stock  = c.hasParameterValue("stock");
        var limit  = c.hasParameterValue("limit");
        var offset = c.hasParameterValue("offset");

        var t = c.template("demo/queries");
        t.setValue("run_url", c.urlFor(c.route()).toString());
        t.setValue("db_" + db[0], " selected");
        t.setValue("db_label", db[1]);
        if (stock)  t.setValue("stock_checked", " checked");
        if (limit)  t.setValue("limit_checked", " checked");
        if (offset) t.setValue("offset_checked", " checked");

        // the fluent chain that matches the toggles, shown as the query you write
        var code = new StringBuilder("new Select(datasource)\n    .from(\"book\")\n    .field(\"title\").field(\"author\")");
        if (stock)  code.append("\n    .where(\"in_stock\", \"=\", true)");
        if (limit)  code.append("\n    .limit(5)");
        if (offset) code.append("\n    .offset(10)");
        code.append("\n    .getSql()");
        t.setValueEncoded("code", code.toString());

        try {
            t.setValueEncoded("sql", select(ds, stock, limit, offset).getSql());
        } catch (UnsupportedSqlFeatureException e) {
            // this database has no LIMIT or OFFSET keyword, RIFE2 refuses to emit
            // SQL it cannot run, so we drop those clauses and explain why
            t.setValueEncoded("sql", select(ds, stock, false, false).getSql());
            t.setValueEncoded("unsupported", db[1]);
            t.setBlock("note", "note_unsupported");
        }

        c.printHtmxFragment(t, "stage");
    }

    private static Select select(Datasource ds, boolean stock, boolean limit, boolean offset) {
        var q = new Select(ds).from("book").field("title").field("author");
        if (stock)  q.where("in_stock", "=", true);
        if (limit)  q.limit(5);
        if (offset) q.offset(10);
        return q;
    }

    private static String[] database(String key) {
        for (var db : DATABASES) {
            if (db[0].equals(key)) {
                return db;
            }
        }
        return DATABASES[0];
    }
}
