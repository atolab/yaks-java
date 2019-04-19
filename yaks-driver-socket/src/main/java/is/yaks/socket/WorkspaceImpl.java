package is.yaks.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;

import is.yaks.Listener;
import is.yaks.Message;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.Yaks;
import is.yaks.socket.messages.MessageFactory;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.utils.MessageCode;

public class WorkspaceImpl implements Workspace {

	public Path path =  null;
	public int wsid = 0;


	private static WorkspaceImpl instance;
	Yaks yaks = YaksImpl.getInstance();
	SocketChannel sock = yaks.getChannel();

	public WorkspaceImpl(){
		Yaks yaks = YaksImpl.getInstance();
		sock = yaks.getChannel();
	}

	public static synchronized Workspace getInstance() 
	{
		if( instance == null ) 
		{
			instance = new WorkspaceImpl();
		}
		return instance;
	}

	@Override
	public boolean put(Path p, Value v, int quorum) {
		int vle = 0;
		boolean is_put_ok = false;

		try {	
			Message putM = new  MessageFactory().getMessage(MessageCode.PUT, null);
			putM.add_property(Message.WSID, String.valueOf(wsid));
			putM.add_workspace(p, v);
			if(sock != null) {
				putM.write(sock, putM);
				//==>			
				while(vle == 0) {
					vle = VLEEncoder.read_vle(sock);

				}
				if(vle > 0) {
					//	read response msg
					ByteBuffer buffer = ByteBuffer.allocate(vle);
					sock.read(buffer);
					Message msgReply = putM.read(buffer);
					if(msgReply.getCorrelationID() == putM.getCorrelationID()) {
						if((msgReply) !=null && msgReply.getMessageCode().equals(MessageCode.OK)) 
						{
							is_put_ok = true;
						}
					}
				}
			} else {
				System.out.println("ERROR: socket is null!");
			}

		} catch (IOException e) {
			e.printStackTrace();
		} 
		return is_put_ok;
	}

	@Override
	public void update() {

	}

	@Override
	public Map<Path, Value> get(Selector select) {
		int vle = 0;
		Map<Path, Value> kvs = null;
		try {	
			Message getM = new  MessageFactory().getMessage(MessageCode.GET, null);
			getM.add_property(Message.WSID, String.valueOf(wsid));
			getM.add_selector(select);
			if(sock != null) {			
				getM.write(sock, getM);
				//==>			
				while(vle == 0) {
					vle = VLEEncoder.read_vle(sock);

				}
				if(vle > 0) {
					//	read response msg
					ByteBuffer buffer = ByteBuffer.allocate(vle);
					sock.read(buffer);
					Message msgReply = getM.read(buffer);
					if(msgReply.getCorrelationID() == getM.getCorrelationID()) {
						if(msgReply !=null && msgReply.getMessageCode().equals(MessageCode.VALUES)) 
						{	
							if(!msgReply.getValuesList().isEmpty())
							{
								kvs = msgReply.getValuesList();
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return kvs;
	}

	@Override
	public boolean remove(Path path, int quorum) {
		int vle = 0;
		boolean is_remove_ok = false;
		try {	
			Message deleteM = new  MessageFactory().getMessage(MessageCode.DELETE, null);
			deleteM.add_property(Message.WSID, String.valueOf(wsid));
			deleteM.setPath(path);
			if(sock != null) {
				deleteM.write(sock, deleteM);

				//==>			
				while(vle == 0) {
					vle = VLEEncoder.read_vle(sock);

				}
				if(vle > 0) {
					//	read response msg
					ByteBuffer buffer = ByteBuffer.allocate(vle);
					sock.read(buffer);
					Message msgReply = deleteM.read(buffer);
					if(msgReply.getCorrelationID() == deleteM.getCorrelationID()) {
						if(msgReply.getMessageCode().equals(MessageCode.OK)) 
						{	
							is_remove_ok = true;
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return is_remove_ok;
	}

	@Override
	public String subscribe(Selector selector, Listener listener) {
		int vle = 0;
		String subid = "";
		try {	
			Message subscribeM = new  MessageFactory().getMessage(MessageCode.SUB, null);
			subscribeM.add_property(Message.WSID, String.valueOf(wsid));
			subscribeM.setPath(Path.ofString(selector.toString()));
			if(sock != null) {
				subscribeM.write(sock, subscribeM);
				//==>			
				while(vle == 0) {
					vle = VLEEncoder.read_vle(sock);

				}
				if(vle > 0) {
					//	read response msg
					ByteBuffer buffer = ByteBuffer.allocate(vle);
					sock.read(buffer);
					Message msgReply = subscribeM.read(buffer);
					if(msgReply.getCorrelationID() == subscribeM.getCorrelationID()) {
						if(msgReply.getMessageCode().equals(MessageCode.OK)) 
						{
							subid = (String)msgReply.getPropertiesList().get(Message.SUBID);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return subid;
	}

	@Override
	public String subscribe(Selector selector) {
		int vle = 0;
		String subid = "";
		try {	
			Message subscribeM = new  MessageFactory().getMessage(MessageCode.SUB, null);
			subscribeM.add_property(Message.WSID, String.valueOf(wsid));
			subscribeM.setPath(Path.ofString(selector.toString()));
			if(sock != null) {
				subscribeM.write(sock, subscribeM);
				//==>			
				while(vle == 0) {
					vle = VLEEncoder.read_vle(sock);

				}
				if(vle > 0) {
					//	read response msg
					ByteBuffer buffer = ByteBuffer.allocate(vle);
					sock.read(buffer);
					Message msgReply = subscribeM.read(buffer);
					if(msgReply.getCorrelationID() == subscribeM.getCorrelationID()) {
						if(msgReply.getMessageCode().equals(MessageCode.OK)) 
						{
							subid = (String)msgReply.getPropertiesList().get(Message.SUBID);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return subid;
	}

	@Override
	public boolean unsubscribe(String subid) {
		int vle = 0;
		boolean is_unsub_ok = false;
		try {	
			Message unsubM = new  MessageFactory().getMessage(MessageCode.UNSUB, null);
			unsubM.add_property(Message.WSID, String.valueOf(wsid));
			unsubM.setPath(Path.ofString(subid.toString()));
			if(sock != null) {
				unsubM.write(sock, unsubM);
				//==>			
				while(vle == 0) {
					vle = VLEEncoder.read_vle(sock);

				}
				if(vle > 0) {
					//	read response msg
					ByteBuffer buffer = ByteBuffer.allocate(vle);
					sock.read(buffer);
					Message msgReply = unsubM.read(buffer);
					if(msgReply.getCorrelationID() == unsubM.getCorrelationID()) {
						if(msgReply.getMessageCode().equals(MessageCode.OK)) 
						{
							is_unsub_ok = true;
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return is_unsub_ok;
	}

	@Override
	public void register_eval(Path path, Listener evcb) {
		int vle = 0;
		boolean is_reg_eval_ok = false;
		try {	

			Message regEvalM = new  MessageFactory().getMessage(MessageCode.REG_EVAL, null);
			regEvalM.add_property(Message.WSID, String.valueOf(wsid));
			regEvalM.setPath(Path.ofString(path.toString()));
			if(sock != null) {
				regEvalM.write(sock, regEvalM);
				//==>			
				while(vle == 0) {
					vle = VLEEncoder.read_vle(sock);

				}
				if(vle > 0) {
					//	read response msg
					ByteBuffer buffer = ByteBuffer.allocate(vle);
					sock.read(buffer);
					Message msgReply = regEvalM.read(buffer);
					if(msgReply.getCorrelationID() == regEvalM.getCorrelationID()) {
						if(msgReply.getMessageCode().equals(MessageCode.OK)) 
						{
							is_reg_eval_ok = true;
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	@Override
	public void unregister_eval(Path path) {
		int vle = 0;
		boolean is_unreg_eval_ok = false;
		try {	
			Message unreg_evalM = new  MessageFactory().getMessage(MessageCode.UNREG_EVAL, null);
			unreg_evalM.add_property(Message.WSID, String.valueOf(wsid));
			unreg_evalM.setPath(Path.ofString(path.toString()));
			if(sock != null) {
				unreg_evalM.write(sock, unreg_evalM);
				//==>			
				while(vle == 0) {
					vle = VLEEncoder.read_vle(sock);

				}
				if(vle > 0) {
					//	read response msg
					ByteBuffer buffer = ByteBuffer.allocate(vle);
					sock.read(buffer);
					Message msgReply = unreg_evalM.read(buffer);
					if(msgReply.getCorrelationID() == unreg_evalM.getCorrelationID()) {
						if(msgReply.getMessageCode().equals(MessageCode.OK)) 
						{
							is_unreg_eval_ok = true;
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	@Override
	public String eval(Selector selector) {
		int vle = 0;
		String values = "";
		try {	
			Message evalM = new  MessageFactory().getMessage(MessageCode.EVAL, null);
			evalM.add_property(Message.WSID, String.valueOf(wsid));
			evalM.setSelector(selector);
			if(sock != null) {
				evalM.write(sock, evalM);
				//==>			
				while(vle == 0) {
					vle = VLEEncoder.read_vle(sock);

				}
				if(vle > 0) {
					//	read response msg
					ByteBuffer buffer = ByteBuffer.allocate(vle);
					sock.read(buffer);
					Message msgReply = evalM.read(buffer);
					if(msgReply.getCorrelationID() == evalM.getCorrelationID()) {
						if(msgReply != null && msgReply.getMessageCode().equals(MessageCode.VALUES)) 
						{
							if(!msgReply.getValuesList().isEmpty()){

								for (Map.Entry<Path, Value> pair : msgReply.getValuesList().entrySet()) {
									values += "(" + pair.getKey().toString() +", " +  pair.getValue().getValue().toString()+")";
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return values;
	}

	public void setPath(Path p) {
		path = p;
	}

	public Path getPath() {
		return path;
	}

	@Override
	public void setWsid(int id) {
		wsid = id;
	}

	@Override
	public int getWsid() {
		return wsid;
	}
}
