package is.yaks;

import java.util.Properties;

public interface Observer {
    public void onPut(Path key, Value value);

    public void onUpdate(Path key, Value value);

    public void onRemove(Path key);

    public String evalCallback(Path path, Properties properties);
}