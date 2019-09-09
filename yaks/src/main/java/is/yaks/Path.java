package is.yaks;


public class Path implements Comparable<Path> {

    private String path;

    public Path(String p) {
        if (p == null) {
            throw new NullPointerException("The given path is null");
        }
        if (p.isEmpty()) {
            throw new IllegalArgumentException("Invalid path (empty String)");
        }
        for (int i=0; i < p.length(); ++i) {
            char c = p.charAt(i);
            if (c == '?' || c == '#' || c == '[' || c == ']' || c == '*')
            throw new IllegalArgumentException("Invalid path: " + p + " (forbidden character at index " + i + ")");
        }
        this.path = removeUselessSlashes(p);
    }

    private String removeUselessSlashes(String s) {
        String result = s.replaceAll("/+", "/");
        if (result.charAt(result.length()-1) == '/') {
            return result.substring(0, result.length()-1);
        } else {
            return result;
        }
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public int compareTo(Path p) {
        return this.path.compareTo(p.path);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (object instanceof Path) {
            return this.path.equals(((Path) object).path);
        }
        return false;
    }

    public int length() {
        return path.length();
    }

    public boolean isRelative() {
        return length() == 0 || path.charAt(0) != '/';
    }

    public Path addPrefix(Path prefix) {
        return new Path(prefix.toString() + this.path);
    }

}
