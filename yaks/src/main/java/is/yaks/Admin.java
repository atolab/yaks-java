package is.yaks;

import java.util.Collection;
import java.util.Properties;
import java.util.Map;
import java.util.Hashtable;

import is.yaks.Path;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.Yaks;


public class Admin {

    private Workspace w;
    private String yaksid;

    protected Admin(Workspace w, String yaksid) {
        this.w = w;
        this.yaksid = yaksid;
    }

    private Properties propertiesOfValue(Value v) {
        if (v instanceof PropertiesValue) {
            return ((PropertiesValue) v).getProperties();
        } else {
            Properties p = new Properties();
            p.setProperty("value", v.toString());
            return p;
        }
    }

    /************* Backends management *************/

    public void addBackend(String beid, Properties properties) throws YException {
        addBackend(beid, properties, yaksid);
    }

    public void addBackend(String beid, Properties properties, String yaks) throws YException {
        String path = String.format("/@/%s/backend/%s", yaks, beid);
        w.put(new Path(path), new PropertiesValue(properties));
    }

    public Properties getBackend(String beid) throws YException {
        return getBackend(beid, yaksid);
    }

    public Properties getBackend(String beid, String yaks) throws YException {
        String sel = String.format("/@/%s/backend/%s", yaks, beid);
        Collection<PathValue> pvs = w.get(new Selector(sel));
        if (! pvs.iterator().hasNext()) {
            return null;
        } else {
            return propertiesOfValue(pvs.iterator().next().getValue());
        }
    }

    public Map<String, Properties> getBackends() throws YException {
        return getBackends(yaksid);
    }

    public Map<String, Properties> getBackends(String yaks) throws YException {
        String sel = String.format("/@/%s/backend/*", yaks);
        Collection<PathValue> pvs = w.get(new Selector(sel));
        Map<String, Properties> result = new Hashtable<String, Properties>(pvs.size());
        for (PathValue pv : pvs) {
            String beid = pv.getPath().toString().substring(sel.length()-1);
            result.put(beid, propertiesOfValue(pv.getValue()));
        }
        return result;
    }

    public void removeBackend(String beid) throws YException {
        removeBackend(beid, yaksid);
    }

    public void removeBackend(String beid, String yaks) throws YException {
        String path = String.format("/@/%s/backend/%s", yaks, beid);
        w.remove(new Path(path));
    }

    /************* Storages management *************/

    public void addStorage(String stid, Properties properties) throws YException {
        addStorageOnBackend(stid, properties, "auto", yaksid);
    }

    public void addStorage(String stid, Properties properties, String yaks) throws YException {
        addStorageOnBackend(stid, properties, "auto", yaks);
    }

    public void addStorageOnBackend(String stid, Properties properties, String backend) throws YException {
        addStorageOnBackend(stid, properties, backend, yaksid);
    }
    public void addStorageOnBackend(String stid, Properties properties, String backend, String yaks) throws YException {
        String path = String.format("/@/%s/backend/%s/storage/%s", yaks, backend, stid);
        w.put(new Path(path), new PropertiesValue(properties));
    }

    public Properties getStorage(String stid) throws YException {
        return getStorage(stid, yaksid);
    }

    public Properties getStorage(String stid, String yaks) throws YException {
        String sel = String.format("/@/%s/backend/*/storage/%s", yaks, stid);
        Collection<PathValue> pvs = w.get(new Selector(sel));
        if (! pvs.iterator().hasNext()) {
            return null;
        } else {
            return propertiesOfValue(pvs.iterator().next().getValue());
        }
    }

    public Map<String, Properties> getStorages() throws YException {
        return getStoragesFromBackend("*", yaksid);
    }

    public Map<String, Properties> getStorages(String yaks) throws YException {
        return getStoragesFromBackend("*", yaks);
    }

    public Map<String, Properties> getStoragesFromBackend(String backend) throws YException {
        return getStoragesFromBackend(backend, yaksid);
    }

    public Map<String, Properties> getStoragesFromBackend(String backend, String yaks) throws YException {
        String sel = String.format("/@/%s/backend/%s/storage/*", yaks, backend);
        Collection<PathValue> pvs = w.get(new Selector(sel));
        Map<String, Properties> result = new Hashtable<String, Properties>(pvs.size());
        for (PathValue pv : pvs) {
            String stPath = pv.getPath().toString();
            String stid = stPath.substring(stPath.lastIndexOf('/')+1);
            result.put(stid, propertiesOfValue(pv.getValue()));
        }
        return result;
    }

    public void removeStorage(String stid) throws YException {
        removeStorage(stid, yaksid);
    }

    public void removeStorage(String stid, String yaks) throws YException {
        String sel = String.format("/@/%s/backend/*/storage/%s", yaks, stid);
        Collection<PathValue> pvs = w.get(new Selector(sel));
        if (pvs.iterator().hasNext()) {
            Path p = pvs.iterator().next().getPath();
            w.remove(p);
        }
    }

}
