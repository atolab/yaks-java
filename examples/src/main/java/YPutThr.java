import java.util.Arrays;

import io.zenoh.Vle;
import is.yaks.Path;
import is.yaks.StringValue;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.Yaks;

class YPutThr {

    public static void main(String[] args) {
        String locator = "tcp/127.0.0.1:7447";
        if (args.length < 1) {
            System.out.println("USAGE:");
            System.out.println("\tYPutThr [I|W]<payload-size> [<zenoh-locator>]");
            System.out.println("\t\tWhere the optional character in front of payload size means:");
            System.out.println("\t\t\tI : use a non-direct ByteBuffer (created via ByteBuffer.allocate())");
            System.out.println("\t\t\tW : use a wrapped ByteBuffer (created via ByteBuffer.wrap())");
            System.out.println("\t\t\tunset : use a direct ByteBuffer");
            System.exit(-1);
        }

        int len;
        String lenArg = args[0];
        if (lenArg.startsWith("I")) {
            len = Integer.parseInt(lenArg.substring(1));
            data = java.nio.ByteBuffer.allocate(len+8);
            System.out.println("Running throughput test for payload of "+len+" bytes from a non-direct ByteBuffer");
        } else if (lenArg.startsWith("W")) {
            len = Integer.parseInt(lenArg.substring(1));
            byte[] array = new byte[len+8+1024];
            data = java.nio.ByteBuffer.wrap(array, 100, len+8);
            System.out.println("Running throughput test for payload of "+len+" bytes from a wrapped ByteBuffer");
        } else {
            len = Integer.parseInt(lenArg);
            data = java.nio.ByteBuffer.allocateDirect(len+8);
            System.out.println("Running throughput test for payload of "+len+" bytes from a direct ByteBuffer");
        }

        try {
        	String path = "/test/thr";

        	char[] chars = new char[len];
    		Arrays.fill(chars, 'X');

            Path p = new Path(path);

            Value v = new StringValue(new String(chars));

            System.out.println("Login to "+locator+"...");
            Yaks y = Yaks.login(locator, null);

            System.out.println("Use Workspace on '/'");
            Workspace w = y.workspace(new Path("/"));

            System.out.println("Put on "+p+" : "+v.toString().length()+"b");

            while (true) {
                w.put(p, v);
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
