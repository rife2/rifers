package rifers.elements.demo;

import rife.database.Datasource;
import rife.database.DbConnection;
import rife.database.migrations.DbMigrations;
import rife.database.migrations.ReversibleDbMigration;
import rife.database.queries.CreateTable;
import rife.engine.Context;
import rife.engine.Element;
import rife.engine.annotations.Parameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A database-migrations demo that stays completely stateless. Every request
 * spins up a private in-memory H2, runs the real migrations to reach the
 * requested version, reads the resulting schema straight from the database,
 * and destroys it again before responding. Nothing is shared and nothing
 * accumulates, so any number of visitors can run it at once.
 * <p>
 * Stepping down from a higher version genuinely executes the {@code down()}
 * migrations, so rolling back is real, not simulated.
 */
public class MigrationsDemo implements Element {
    static final int MAX_VERSION = 2;
    static final AtomicLong SEQUENCE = new AtomicLong();

    public static class CreateUsers extends ReversibleDbMigration {
        public void up() {
            add(createTable("users")
                .column("id", int.class, CreateTable.NOTNULL)
                .column("login", String.class, 30)
                .primaryKey("id"));
        }

        public void down() {
            add(dropTable("users"));
        }
    }

    public static class AddEmail extends ReversibleDbMigration {
        public void up() {
            add(alterTable("users").addColumn("email", String.class, 100));
            add(createIndex("users_email_idx").table("users").column("email"));
        }

        public void down() {
            add(dropIndex("users_email_idx").table("users"));
            add(alterTable("users").dropColumn("email"));
        }
    }

    @Parameter int to = 1;
    @Parameter int from = 0;

    public void process(Context c) {
        var target = clamp(to);
        var current = clamp(from);

        var datasource = new Datasource("org.h2.Driver", "jdbc:h2:mem:demomig" + SEQUENCE.incrementAndGet(), "sa", "", 5);
        // hold one connection open so the in-memory database stays alive for the
        // request; closing it and cleaning up the pool destroys the database
        var keepAlive = datasource.getConnection();
        Map<String, List<Column>> schema;
        try {
            var migrations = new DbMigrations(datasource)
                .add(1, new CreateUsers())
                .add(2, new AddEmail());
            // reach the higher of the two versions with up() migrations, then roll
            // back down to the target with real down() migrations when stepping back
            var high = Math.max(current, target);
            migrations.migrateFrom(0, high);
            if (target < high) {
                migrations.rollbackFrom(high, target);
            }
            schema = readSchema(keepAlive);
        } finally {
            keepAlive.close();
            datasource.cleanup();
        }

        var t = c.template("demo/migrations");
        t.setValue("version", target);
        t.setValue("max", MAX_VERSION);

        if (schema.isEmpty()) {
            t.setBlock("schema", "schema_empty");
        } else {
            for (var table : schema.entrySet()) {
                t.setValueEncoded("table_name", table.getKey());
                t.setValue("columns", "");
                for (var column : table.getValue()) {
                    t.setValueEncoded("column_name", column.name());
                    t.setValueEncoded("column_type", column.type());
                    t.appendBlock("columns", "schema_column");
                }
                t.appendBlock("schema", "schema_table");
            }
        }

        if (target > 0) {
            t.setValue("back_url", c.urlFor(c.route()).param("to", target - 1).param("from", target).toString());
            t.appendBlock("steps", "step_back");
        }
        if (target < MAX_VERSION) {
            t.setValue("up_url", c.urlFor(c.route()).param("to", target + 1).param("from", target).toString());
            t.appendBlock("steps", "step_up");
        }

        c.printHtmxFragment(t, "stage");
    }

    private static int clamp(int version) {
        return Math.max(0, Math.min(MAX_VERSION, version));
    }

    private Map<String, List<Column>> readSchema(DbConnection connection) {
        var result = new LinkedHashMap<String, List<Column>>();
        try (var columns = connection.getMetaData().getColumns(null, "PUBLIC", null, null)) {
            while (columns.next()) {
                // lower-case the identifiers so they read like the code that
                // wrote them, rather than H2's upper-cased catalog form
                var table = columns.getString("TABLE_NAME").toLowerCase();
                var name = columns.getString("COLUMN_NAME").toLowerCase();
                var type = formatType(columns.getString("TYPE_NAME"), columns.getInt("COLUMN_SIZE"));
                result.computeIfAbsent(table, k -> new ArrayList<>()).add(new Column(name, type));
            }
        } catch (Exception e) {
            throw new RuntimeException("could not read the schema", e);
        }
        return result;
    }

    /** A single column, kept as its own value so the template can lay the
     * schema out as a real table instead of one run-together line. */
    record Column(String name, String type) {}

    private static String formatType(String typeName, int size) {
        // H2 reports the SQL-standard "CHARACTER VARYING"; show the familiar
        // VARCHAR(n) form the developer actually wrote instead
        if ("CHARACTER VARYING".equals(typeName)) {
            return "VARCHAR(" + size + ")";
        }
        return typeName;
    }
}
