import rife.database.Datasource;
import rife.database.migrations.*;
import rife.database.queries.CreateTable;
import rife.resources.DatabaseResourcesFactory;

public class HelloMigrations {
    static class CreateUsers extends ReversibleDbMigration {
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

    static class AddEmail extends ReversibleDbMigration {
        public void up() {
            add(alterTable("users").addColumn("email", String.class, 50));
        }

        public void down() {
            add(alterTable("users").dropColumn("email"));
        }
    }

    public static void main(String[] args) {
        var datasource = new Datasource(
            "org.h2.Driver", "jdbc:h2:./embedded_dbs/h2/hello", "sa", "", 5);
        new DbMigrations(datasource)
            .state(DatabaseResourcesFactory.instance(datasource))
            .add(1, new CreateUsers())
            .add(2, new AddEmail())
            .migrate();
    }
}
