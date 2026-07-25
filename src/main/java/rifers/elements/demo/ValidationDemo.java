package rifers.elements.demo;

import rife.engine.Context;
import rife.engine.Element;
import rife.engine.RequestMethod;
import rife.validation.ConstrainedProperty;
import rife.validation.Validation;
import rife.validation.ValidationError;

/**
 * A dynamic-validation demo. The rules live on the {@link Signup} bean itself
 * as constraints, organised into named groups. The form binds straight to the
 * bean and RIFE2 validates either the whole bean or just one group, returning
 * a precise error for each field that fails.
 */
public class ValidationDemo implements Element {
    /**
     * A constrained bean: its rules are declared once, on the bean, and split
     * into an "account" group and a "profile" group so they can be validated
     * together or on their own.
     */
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

        public void setName(String name)     { this.name = name; }
        public String getName()              { return name; }
        public void setEmail(String email)   { this.email = email; }
        public String getEmail()             { return email; }
        public void setAge(Integer age)      { this.age = age; }
        public Integer getAge()              { return age; }
    }

    public void process(Context c) {
        var group = c.parameter("group", "");

        var t = c.template("demo/validation");
        t.setValue("run_url", c.urlFor(c.route()).toString());
        // keep exactly what was typed in the form
        t.setValueEncoded("name", c.parameter("name", ""));
        t.setValueEncoded("email", c.parameter("email", ""));
        t.setValueEncoded("age", c.parameter("age", ""));
        t.setValue("grp_" + switch (group) {
            case "account", "profile" -> group;
            default -> "all";
        }, " selected");

        if (c.method() == RequestMethod.POST) {
            var signup = c.parametersBean(Signup.class);
            // validate the whole bean, or only the chosen group
            var valid = switch (group) {
                case "account", "profile" -> signup.validateGroup(group);
                default -> signup.validate();
            };
            if (valid) {
                t.setBlock("result", "result_ok");
            } else {
                // one inline error per field that failed its constraints
                for (var error : signup.getValidationErrors()) {
                    t.setValueEncoded("verror_msg", messageFor(error));
                    t.appendBlock("error_" + error.getSubject(), "error_msg");
                }
            }
        }

        c.printHtmxFragment(t, "stage");
    }

    static String messageFor(ValidationError error) {
        var mandatory = ValidationError.IDENTIFIER_MANDATORY.equals(error.getIdentifier());
        return switch (error.getSubject()) {
            case "name" -> mandatory ? "Name is required" : "Name must be 2 to 20 characters";
            case "email" -> mandatory ? "Email is required" : "Email must be a valid address";
            case "age" -> mandatory ? "Age is required" : "Age must be a whole number from 18 to 120";
            default -> error.getSubject() + " is not valid";
        };
    }
}
