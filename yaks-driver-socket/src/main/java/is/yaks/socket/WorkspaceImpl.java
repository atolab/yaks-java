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
import is.yaks.YaksRuntime;
import is.yaks.socket.async.YaksImpl;
import is.yaks.socket.messages.MessageFactory;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.utils.MessageCode;

public class WorkspaceImpl implements Workspace {

	public Path path =  null;
	public int wsid = 0;

	private static WorkspaceImpl instance;
	YaksRuntime rt = null;

	SocketChannel sock = YaksImpl.getChannel();

	public WorkspaceImpl(){}

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

		boolean is_put_ok = false;

		try {	
			Message putM = new  MessageFactory().getMessage(MessageCode.PUT, null);
			putM.add_property(Message.WSID, String.valueOf(wsid));
			putM.add_workspace(p, v);
			putM.write(sock, putM);
			//==>			
			int vle = 0;
			while(vle == 0) {
				vle = VLEEncoder.read_vle(sock);
				Thread.sleep(YaksImpl.TIMEOUT);
			}
			if(vle > 0) {
				//	read response msg
				System.out.println("==> [vle-put]:" +vle);
				ByteBuffer buffer = ByteBuffer.allocate(vle);
				sock.read(buffer);
				Message msgReply = putM.read(buffer);

				if(msgReply !=null && msgReply.getMessageCode().equals(MessageCode.OK)) 
				{
					is_put_ok = true;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return is_put_ok;
	}

	@Override
	public void update() {

	}

	@Override
	public Map<Path, Value> get(Selector select) {

		Map<Path, Value> kvs = null;
		try {	
			Message getM = new  MessageFactory().getMessage(MessageCode.GET, null);
			getM.add_property(Message.WSID, String.valueOf(wsid));
			getM.add_selector(select);
			getM.write(sock, getM);
			//==>			
			int vle = 0;
			while(vle == 0) {
				vle = VLEEncoder.read_vle(sock);
				Thread.sleep(YaksImpl.TIMEOUT);
			}
			if(vle > 0) {
				//	read response msg
				System.out.println("==> [vle-get]:" +vle);
				ByteBuffer buffer = ByteBuffer.allocate(vle);
				sock.read(buffer);
				Message msgReply = getM.read(buffer);
				if(msgReply !=null && msgReply.getMessageCode().equals(MessageCode.VALUES)) 
				{	
					if(!msgReply.getValuesList().isEmpty())
					{
						kvs = msgReply.getValuesList();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return kvs;
	}

	@Override
	public boolean remove(Path path, int quorum) {
		boolean is_remove_ok = false;
		try {	
			Message deleteM = new  MessageFactory().getMessage(MessageCode.DELETE, null);
			deleteM.add_property(Message.WSID, String.valueOf(wsid));
			deleteM.setPath(path);
			deleteM.write(sock, deleteM);

			//==>			
			int vle = 0;
			while(vle == 0) {
				vle = VLEEncoder.read_vle(sock);
				Thread.sleep(YaksImpl.TIMEOUT);
			}
			if(vle > 0) {
				//	read response msg
				System.out.println("==> [vle-get]:" +vle);
				ByteBuffer buffer = ByteBuffer.allocate(vle);
				sock.read(buffer);
				Message msgReply = deleteM.read(buffer);

				if(msgReply.getMessageCode().equals(MessageCode.OK)) 
				{	
					is_remove_ok = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return is_remove_ok;
	}

	@Override
	public String subscribe(Selector selector, Listener listener) {
		String subid = "";
		try {	
			Message subscribeM = new  MessageFactory().getMessage(MessageCode.SUB, null);
			subscribeM.add_property(Message.WSID, String.valueOf(wsid));
			subscribeM.setPath(Path.ofString(selector.toString()));
			subscribeM.write(sock, subscribeM);
			//==>			
			int vle = 0;
			while(vle == 0) {
				vle = VLEEncoder.read_vle(sock);
				Thread.sleep(YaksImpl.TIMEOUT);
			}
			if(vle > 0) {
				//	read response msg
				System.out.println("==> [vle-subscribe]:" +vle);
				ByteBuffer buffer = ByteBuffer.allocate(vle);
				sock.read(buffer);
				Message msgReply = subscribeM.read(buffer);
				if(msgReply.getMessageCode().equals(MessageCode.OK)) 
				{
					subid = (String)msgReply.getPropertiesList().get(Message.SUBID);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return subid;
	}

	@Override
	public String subscribe(Selector selector) {
		String subid = "";
		try {	
			Message subscribeM = new  MessageFactory().getMessage(MessageCode.SUB, null);
			subscribeM.add_property(Message.WSID, String.valueOf(wsid));
			subscribeM.setPath(Path.ofString(selector.toString()));
			subscribeM.write(sock, subscribeM);
			//==>			
			int vle = 0;
			while(vle == 0) {
				vle = VLEEncoder.read_vle(sock);
				Thread.sleep(YaksImpl.TIMEOUT);
			}
			if(vle > 0) {
				//	read response msg
				System.out.println("==> [vle-subscribe]:" +vle);
				ByteBuffer buffer = ByteBuffer.allocate(vle);
				sock.read(buffer);
				Message msgReply = subscribeM.read(buffer);

				if(msgReply.getMessageCode().equals(MessageCode.OK)) 
				{
					subid = (String)msgReply.getPropertiesList().get(Message.SUBID);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
		return subid;
	}

	@Override
	public boolean unsubscribe(String subid) {
		boolean is_unsub_ok = false;
		try {	
			Message unsubM = new  MessageFactory().getMessage(MessageCode.UNSUB, null);
			unsubM.add_property(Message.WSID, String.valueOf(wsid));
			unsubM.setPath(Path.ofString(subid.toString()));
			unsubM.write(sock, unsubM);
			//==>			
			int vle = 0;
			while(vle == 0) {
				vle = VLEEncoder.read_vle(sock);
				Thread.sleep(YaksImpl.TIMEOUT);
			}
			if(vle > 0) {
				//	read response msg
				System.out.println("==> [vle-unsub]:" +vle);
				ByteBuffer buffer = ByteBuffer.allocate(vle);
				sock.read(buffer);
				Message msgReply = unsubM.read(buffer);

				if(msgReply.getMessageCode().equals(MessageCode.OK)) 
				{
					is_unsub_ok = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
		return is_unsub_ok;
	}

	@Override
	public void register_eval(Path path, Listener evcb) {
		try {	

			Message regEvalM = new  MessageFactory().getMessage(MessageCode.REG_EVAL, null);
			regEvalM.add_property(Message.WSID, String.valueOf(wsid));
			regEvalM.setPath(Path.ofString(path.toString()));
			regEvalM.write(sock, regEvalM);
			//==>			
			int vle = 0;
			while(vle == 0) {
				vle = VLEEncoder.read_vle(sock);
				Thread.sleep(YaksImpl.TIMEOUT);
			}
			if(vle > 0) {
				//	read response msg
				System.out.println("==> [vle-regeval]:" +vle);
				ByteBuffer buffer = ByteBuffer.allocate(vle);
				sock.read(buffer);
				regEvalM.read(buffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unregister_eval(Path path) {
		try {	

			Message unreg_evalM = new  MessageFactory().getMessage(MessageCode.UNREG_EVAL, null);
			unreg_evalM.add_property(Message.WSID, String.valueOf(wsid));
			unreg_evalM.setPath(Path.ofString(path.toString()));
			unreg_evalM.write(sock, unreg_evalM);
			//==>			
			int vle = 0;
			while(vle == 0) {
				vle = VLEEncoder.read_vle(sock);
				Thread.sleep(YaksImpl.TIMEOUT);
			}
			if(vle > 0) {
				//	read response msg
				System.out.println("==> [vle-unreg_eval]:" +vle);
				ByteBuffer buffer = ByteBuffer.allocate(vle);
				sock.read(buffer);
				unreg_evalM.read(buffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String eval(Selector selector) {
		String values = "";
		try {	
			Message evalM = new  MessageFactory().getMessage(MessageCode.EVAL, null);
			evalM.add_property(Message.WSID, String.valueOf(wsid));
			evalM.setSelector(selector);
			evalM.write(sock, evalM);
			//==>			
			int vle = 0;
			while(vle == 0) {
				vle = VLEEncoder.read_vle(sock);
				Thread.sleep(YaksImpl.TIMEOUT);
			}
			if(vle > 0) {
				//	read response msg
				System.out.println("==> [vle-eval]:" +vle);
				ByteBuffer buffer = ByteBuffer.allocate(vle);
				sock.read(buffer);
				Message msgReply = evalM.read(buffer);

				if(msgReply != null && msgReply.getMessageCode().equals(MessageCode.VALUES)) 
				{
					if(!msgReply.getValuesList().isEmpty()){

						for (Map.Entry<Path, Value> pair : msgReply.getValuesList().entrySet()) {
							values += "(" + pair.getKey().toString() +", " +  pair.getValue().getValue().toString()+")";
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
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
