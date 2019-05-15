package is.yaks.socket.qualif;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import is.yaks.Path;
import is.yaks.Value;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;
import is.yaks.socket.async.AsyncYaksImpl;

public class LatencyTest {

    public static int DEFAUL_PORT = 7887;

    private static AsyncYaks yaks;
    private static AsyncWorkspace ws;

    public void init(Properties properties) throws InterruptedException, ExecutionException {
        yaks = AsyncYaksImpl.getInstance();
        if (yaks != null) {
            // login to yaks api
            yaks = yaks.login(properties);
            // creates workspace
            ws = yaks.workspace(Path.ofString("/"));
        }
    }

    public void put_n(int n, AsyncWorkspace ws, Path path, Value val) {
        if (n > 1) {
            ws.put(path, val, 0);
            put_n(n - 1, ws, path, val);
        } else
            ws.put(path, val, 0);
    }

    private static String create_data(int size) {
        char[] chars = new char[size];
        Arrays.fill(chars, 'a');
        return new String(chars);
    }

    public void runPut(int samples, int size) {
        Path path = Path.ofString("test/thr/put");
        Value val = new Value(create_data(size));
        long start = System.currentTimeMillis();
        put_n(samples, ws, path, val);
        long stop = System.currentTimeMillis();
        long delta = stop - start;
        System.out.println("Throughput: " + ((double) samples / delta) * 1000 + " msg/sec");
    }

    public static void main(String[] args) {
        LatencyTest lt = new LatencyTest();
        try {
            String host, port;
            int samples, size;
            if (args.length == 4) {
                host = args[0];
                port = args[1];
                samples = Integer.parseInt(args[2]);
                size = Integer.parseInt(args[3]);
                Properties properties = new Properties();
                properties.setProperty("host", host);
                properties.setProperty("port", port);
                // properties.setProperty("cacheSize", "2048");
                lt.init(properties);
                long start = System.currentTimeMillis();
                lt.runPut(samples, size);
                long stop = System.currentTimeMillis();
                long delta = stop - start;
                System.out
                        .println("Latency: " + samples + " puts of size " + size + "bytes, time: " + delta + " ms \n");
            } else {
                System.out.println("Use i.e. java LatencyTest 127.0.0.1 7887 10000 1024 \n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
