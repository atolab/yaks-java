package is.yaks.socket.types;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.YSelector;
import is.yaks.Value;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;

public class MessageImpl implements Message {

    private static MessageImpl instance = null;
    // 1VLE max 64bit
    static VLEEncoder encoder;
    // vle length
    public static long vle_length;
    public static int vle_bytes;
    // 1VLE max 64bit
    static long correlation_id;
    // 8bit
    static int flags;
    // 1bit
    static int flag_a;
    // 1bit
    static int flag_s;
    // 1bit
    static int flag_p;

    int quorum = 0;
    int wsid = 0;
    String subid = "";

    Path path;
    Value value;

    ByteBuffer data;
    YSelector selector;
    Encoding encoding;
    MessageCode messageCode;

    Map<Path, Value> valuesList;
    Map<String, String> dataList;
    Map<Path, Value> workspaceList;
    Map<String, String> propertiesList;

    public MessageImpl() {

        // 8bit (1byte) comprises A,S,P (0xFF = 0b11111111)
        flags = 0;
        // 1bit. Initialized to false.
        flag_a = 0;
        // 1bit. Initialized to false.
        flag_s = 0;
        // 1bit. Initialized to false.
        flag_p = 0;
        // vle_bytes
        vle_bytes = 10;
        // vle length
        vle_length = (64 * 1024);
        // messageCode
        messageCode = null;
        // Correction is VLEEncoder max 64bit
        correlation_id = Utils.generate_correlation_id();
        // VLE encoder
        encoder = VLEEncoder.getInstance();
        // dataList
        dataList = new HashMap<String, String>();
        // propertiesList
        propertiesList = new HashMap<String, String>();
        // worskpaceList
        workspaceList = new HashMap<Path, Value>();
        // valuesList
        valuesList = new HashMap<Path, Value>();
        //

    }

    /**
     * Get instance of the Message implementation
     * 
     * @return MessageImpl
     */
    public static MessageImpl getInstance() {
        if (instance == null) {
            instance = new MessageImpl();
        }
        return instance;
    }

    @Override
    public MessageCode getMessageCode() {
        return messageCode;
    }

    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public long getCorrelationID() {
        return correlation_id;
    }

    @Override
    public Map<Path, Value> getValuesList() {
        return valuesList;
    }

    @Override
    public Map<String, String> getPropertiesList() {
        return propertiesList;
    }

    @Override
    public Map<Path, Value> getWorkspaceList() {
        return workspaceList;
    }

    public void setFlag_a() {
        flag_a = 1;
        flags = (int) (flags | 0x04);
    }

    public void setFlag_s() {
        flag_s = 1;
        flags = (int) (flags | 0x02);
    }

    public void setFlag_p() {
        flag_p = 1;
        flags = (int) (flags | 0x01);
    }

    public void setFlags(int flgs) {
        flags = flgs;
    }

    @Override
    public void add_value(Path path, Value val) {
        valuesList.put(path, val);
    }

    @Override
    public void add_property(String key, String value) {
        setFlag_p();
        propertiesList.put(key, value);
    }

    @Override
    public void add_workspace(Path p, Value v) {
        // setFlag_p();
        workspaceList.put(p, v);
    }

    @Override
    public void add_selector(YSelector select) {
        selector = select;
    }

    public String pprint() {
        String pretty = "\n############ YAKS FE SOCKET MESSAGE ###################" + "\n# CODE: " + messageCode + "\n"
                + "\n# CORR.ID:" + correlation_id + "\n" + "\n# LENGTH: " + vle_length + "\n" + "\n# FLAGS: RAW: "
                + (byte) flags + " | " + "A:" + flag_a + " S:" + flag_s + " P:" + flag_p + "'.\n";

        if (flag_p == 1) {
            pretty = pretty + "\n# HAS PROPERTIES\n# NUMBER OF PROPERTIES: " + propertiesList.size();

            for (Map.Entry<String, String> entry : propertiesList.entrySet()) {
                pretty = pretty + "\n#========\n# " + " KEY: " + entry.getKey() + " VALUE: " + entry.getValue();
            }
        }
        pretty = pretty + "\n#========\nDATA:" + data.toString()
                + "\n#######################################################";
        return pretty;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public void setPath(Path p) {
        this.path = p;
    }

    @Override
    public YSelector getSelector() {
        return selector;
    }

    @Override
    public void setSelector(YSelector s) {
        selector = s;
    }

    @Override
    public Value getValue() {
        return value;
    }

    @Override
    public void setValue(Value val) {
        value = val;
    }

    @Override
    public void setCorrelationId(long corr_id) {
        correlation_id = corr_id;
    }

    @Override
    public String getSubid() {
        return subid;
    }

    @Override
    public void setSubid(String subid) {
        this.subid = subid;
    }
}

class HeaderMessage implements Header {

    public static int P_FLAG = 0x01;
    public static int FLAGS_MASK = 0x01;

    static int flags;
    static int correlation_id;
    static Properties properties;

    public HeaderMessage(int corr_id, int f, Properties prop) {
        correlation_id = corr_id;
        flags = f;
        properties = prop;
    }

    @Override
    public boolean has_flag(int h, int f) {
        return (h & f) != 0;
    }

    @Override
    public boolean has_properties() {
        return (flags & HeaderMessage.P_FLAG) != 0;
    }
}

class LoginMessage extends MessageImpl {

    public LoginMessage() {
        messageCode = MessageCode.LOGIN;
        flags = 0x0;
    }

    public LoginMessage(Properties properties) {
        messageCode = MessageCode.LOGIN;
        flags = 0x0;
        if (properties != null) {
            String username = (String) properties.get("username");
            String password = (String) properties.get("password");
            if (username != null && !username.equals("None") && password != null && !password.equals("None")) {
                add_property("yaks.login", "" + username + ":" + password + "");
            }
        }
    }
}

class LogoutMessage extends MessageImpl {

    public LogoutMessage() {
        messageCode = MessageCode.LOGOUT;
    }
}

class WorkspaceMessage extends MessageImpl {

    public WorkspaceMessage() {

        messageCode = MessageCode.WORKSPACE;
    }

    public WorkspaceMessage(Path path) {

        messageCode = MessageCode.WORKSPACE;

        add_property("path", path.toString());

    }
}

class PutMessage extends MessageImpl {

    public PutMessage() {
        messageCode = MessageCode.PUT;
    }

    public PutMessage(Properties properties) {

        messageCode = MessageCode.PUT;
        quorum = 1;

    }
}

class GetMessage extends MessageImpl {

    public GetMessage() {
        messageCode = MessageCode.GET;
    }
}

class UpdateMessage extends MessageImpl {

    public UpdateMessage() {
        messageCode = MessageCode.UPDATE;
    }
}

class DeleteMessage extends MessageImpl {

    public DeleteMessage() {
        messageCode = MessageCode.DELETE;
    }
}

class SubscribeMessage extends MessageImpl {

    public SubscribeMessage() {
        messageCode = MessageCode.SUB;
    }
}

class UnsubscribeMessage extends MessageImpl {

    public UnsubscribeMessage() {
        messageCode = MessageCode.UNSUB;
    }
}

class NotifyMessage extends MessageImpl {

    public NotifyMessage() {
        messageCode = MessageCode.NOTIFY;
    }
}

class EvalMessage extends MessageImpl {

    public EvalMessage() {
        messageCode = MessageCode.EVAL;
    }
}

class RegisterEvalMessage extends MessageImpl {

    public RegisterEvalMessage() {
        messageCode = MessageCode.REG_EVAL;
    }
}

class UnregisterEvalMessage extends MessageImpl {

    public UnregisterEvalMessage() {
        messageCode = MessageCode.UNREG_EVAL;
    }
}

class ValuesMessage extends MessageImpl {

    public ValuesMessage() {
        messageCode = MessageCode.VALUES;
    }
}

class OkMessage extends MessageImpl {

    public OkMessage() {
        messageCode = MessageCode.OK;
    }
}

class ErrorMessage extends MessageImpl {

    public ErrorMessage() {
        messageCode = MessageCode.ERROR;
    }
}
