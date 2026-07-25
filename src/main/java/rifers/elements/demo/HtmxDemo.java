package rifers.elements.demo;

import rife.engine.Context;
import rife.engine.Element;
import rife.engine.annotations.Parameter;

import java.util.List;

/**
 * An htmx demo. One element answers both a browser and htmx: a normal request
 * gets the whole page, and each keystroke in the search box is an htmx request
 * that gets back only the updated list block, straight from a template. It
 * uses the context's {@code printHtmxFragment}, choosing which block to swap,
 * with no client-side rendering.
 */
public class HtmxDemo implements Element {
    // a small fixed list to filter over
    static final List<String> LANGUAGES = List.of(
        "Java", "Kotlin", "Scala", "Groovy", "Clojure",
        "Python", "Ruby", "Rust", "Go", "TypeScript", "Swift", "Elixir");

    @Parameter String q = "";

    public void process(Context c) {
        var t = c.template("demo/htmx");
        t.setValue("run_url", c.urlFor(c.route()).toString());
        t.setValueEncoded("q", q);

        var query = q.trim().toLowerCase();
        var matches = LANGUAGES.stream()
            .filter(language -> language.toLowerCase().contains(query))
            .toList();
        if (matches.isEmpty()) {
            t.setBlock("rows", "no_match");
        } else {
            for (var language : matches) {
                t.setValueEncoded("language", language);
                t.appendBlock("rows", "row");
            }
        }

        // on a live htmx request, a keystroke with a query swaps just the list
        // block, while the first load swaps the whole stage; a browser or an
        // htmx history restore gets the full page instead. printHtmxFragment
        // makes that choice and declares the Vary on both htmx headers
        c.printHtmxFragment(t, c.hasParameterValue("q") ? "rows_fragment" : "stage");
    }
}
