import rife.engine.*;
import rife.validation.*;

public class HelloValidation extends Site {
    // the rules live on the bean, split into named groups (or keep the bean a
    // plain POJO and declare them in a SignupMetaData class, merged in by RIFE2)
    public static class Signup extends Validation {
        private String name;
        private String email;
        private Integer age;

        public void activateValidation() {
            addGroup("account")
                .addConstraint(new ConstrainedProperty("name")
                    .notNull(true).minLength(2).maxLength(20))
                .addConstraint(new ConstrainedProperty("email")
                    .notNull(true).email(true));
            addGroup("profile")
                .addConstraint(new ConstrainedProperty("age")
                    .notNull(true).rangeBegin(18).rangeEnd(120));
        }

        public void setName(String name)   { this.name = name; }
        public String getName()            { return name; }
        public void setEmail(String email) { this.email = email; }
        public String getEmail()           { return email; }
        public void setAge(Integer age)    { this.age = age; }
        public Integer getAge()            { return age; }
    }

    public void setup() {
        get("/signup", c -> c.print(c.template("signup")));

        post("/signup", c -> {
            var signup = c.parametersBean(Signup.class);
            var group = c.parameter("group", "");
            // validate the whole bean, or just one group
            var valid = group.isEmpty()
                ? signup.validate()
                : signup.validateGroup(group);
            if (valid) {
                c.print("Welcome, " + signup.getName());
            } else {
                signup.getValidationErrors().forEach(e ->
                    c.print(e.getSubject() + " " + e.getIdentifier() + "<br>"));
            }
        });
    }

    public static void main(String[] args) {
        new Server().start(new HelloValidation());
    }
}
