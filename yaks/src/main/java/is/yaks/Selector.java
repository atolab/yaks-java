package is.yaks;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public final class Selector implements Comparable<Selector> {

    private static final String REGEX_PATH = "[^\\[\\]?#]+";
    private static final String REGEX_PREDICATE = "[^\\[\\]\\(\\)#]+";
    private static final String REGEX_PROPERTIES = ".*";
    private static final String REGEX_FRAGMENT = ".*";
    private static final Pattern PATTERN = Pattern.compile(
        String.format("(%s)(\\?(%s)?(\\((%s)\\))?)?(#(%s))?", REGEX_PATH, REGEX_PREDICATE, REGEX_PROPERTIES, REGEX_FRAGMENT));


    private String path;
    private String predicate;
    private String properties;
    private String fragment;
    private String optionalPart;
    private String toString;


    public Selector(String s) throws IllegalArgumentException {
        if (s == null) {
            throw new NullPointerException("The given selector is null");
        }
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Invalid selector (empty String)");
        }

        Matcher m = PATTERN.matcher(s);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid selector (not matching regex)");
        }
        this.path = m.group(1);
        this.predicate = m.group(3);
        if (this.predicate == null) this.predicate = "";
        this.properties = m.group(5);
        if (this.properties == null) this.properties = "";
        this.fragment = m.group(7);
        if (this.fragment == null) this.fragment = "";

        this.optionalPart = String.format("%s%s%s",
            predicate,
            (properties.length() > 0 ? "("+properties+")" : ""),
            (fragment.length() > 0 ? "#"+fragment : ""));
        this.toString = path + (optionalPart.length() > 0 ? "?"+optionalPart : "");
    }

    private Selector(String path, String predicate, String fragment) {
        this.path = path;
        this.predicate = predicate;
        this.fragment = fragment;
        this.toString = path + (predicate.isEmpty() ? "" : "?"+predicate) + (fragment.isEmpty() ? "" : "#"+fragment);
    }

    public String getPath() {
        return path;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getProperties() {
        return properties;
    }

    public String getFragment() {
        return fragment;
    }

    protected String getOptionalPart() {
        return optionalPart;
    }

    @Override
    public String toString() {
        return toString;
    }

    @Override
    public int compareTo(Selector s) {
        return this.toString.compareTo(s.toString);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (object instanceof Selector) {
            return this.toString.equals(((Selector) object).toString);
        }
        return false;
    }

    public boolean isRelative() {
        return path.length() == 0 || path.charAt(0) != '/';
    }

    public Selector addPrefix(Path prefix) {
        return new Selector(prefix.toString() + this.path, this.predicate, this.fragment);
    }

}