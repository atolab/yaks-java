package is.yaks.socket.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import is.yaks.Listener;
import is.yaks.Message;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.YaksRuntime;
import is.yaks.async.Workspace;
import is.yaks.socket.messages.MessageFactory;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.utils.MessageCode;

public class WorkspaceImpl implements Workspace {
	
	public Path path =  null;
	public CompletableFuture<Integer> wsid = null;
	
    private static WorkspaceImpl instance;
    YaksRuntime rt = null;
    
    
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
	public CompletableFuture<Boolean> put(Path p, Value v, int quorum) {
		
		CompletableFuture<Boolean> is_put_ok = new CompletableFuture<Boolean>();

		try {	
			SocketChannel sock = YaksImpl.getChannel();
			
			Message putM = new  MessageFactory().getMessage(MessageCode.PUT, null);
			putM.add_property(Message.WSID, String.valueOf(wsid));
			putM.add_workspace(p, v);
			putM.write(sock, putM);

			//read response msg
			ByteBuffer buffer = ByteBuffer.allocate(1);
			// Returns the number of bytes read, possibly zero, 
			// or -1 if the channel has reached end-of-stream
			int bytesRead = sock.read(buffer);
			while (bytesRead > 0) {
				Message msgReply = putM.read(sock, buffer);
				if(msgReply !=null && msgReply.getMessageCode().equals(MessageCode.OK)) 
				{
					is_put_ok.complete(true);
				}
				buffer.clear();
				bytesRead = sock.read(buffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return is_put_ok;
	}

	@Override
	public CompletableFuture<Void> update() {
		return null;
	}

	@Override
	public CompletableFuture<Map<Path, Value>> get(Selector select, int quorum) {
		
		CompletableFuture<Map<Path, Value>> kvs = null;
		try {	
			SocketChannel sock = YaksImpl.getChannel();
			
			Message getM = new  MessageFactory().getMessage(MessageCode.GET, null);

			getM.add_property(Message.WSID, String.valueOf(wsid));
			getM.add_selector(select);
			getM.write(sock, getM);

			//read response msg
			ByteBuffer buffer = ByteBuffer.allocate(1);
			// Returns the number of bytes read, possibly zero, 
			// or -1 if the channel has reached end-of-stream
			int bytesRead = 0;
			
			bytesRead = sock.read(buffer);
			
			while (bytesRead > 0) {
				
				// read the vle_length one byte at the time
				buffer.flip();
				byte lengthByte =  buffer.get();
				ByteBuffer bb2 = ByteBuffer.allocate(VLEEncoder.MAX_BYTES).order(ByteOrder.BIG_ENDIAN);
				while((lengthByte & VLEEncoder.MORE_BYTES_FLAG) !=0) {
					bb2.put((byte) (lengthByte));
					buffer.clear();
					sock.read(buffer);
					buffer.flip();
					lengthByte = buffer.get();
				}			
				bb2.put(lengthByte);
				bb2.flip();
				
				Message msgReply = getM.read(sock, bb2);
				
				if(msgReply !=null && msgReply.getMessageCode().equals(MessageCode.VALUES)) 
				{	
					if(!((Message) msgReply).getValuesList().isEmpty()){
						
						kvs.complete(msgReply.getValuesList());
					}
				}
				buffer.clear();
				bytesRead = sock.read(buffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return kvs;
	}

	@Override
	public CompletableFuture<Boolean> remove(Path path, int quorum) {
		
		CompletableFuture<Boolean> is_remove_ok = new CompletableFuture<Boolean>();
		
		try {	
			SocketChannel sock = YaksImpl.getChannel();
			
			Message deleteM = new  MessageFactory().getMessage(MessageCode.DELETE, null);

			deleteM.add_property(Message.WSID, String.valueOf(wsid));
			deleteM.setPath(path);
			deleteM.write(sock, deleteM);

			
			//read response msg
			ByteBuffer buffer = ByteBuffer.allocate(1);
			
			
			// Returns the number of bytes read, possibly zero, 
			// or -1 if the channel has reached end-of-stream
			
			while (sock.read(buffer) > 0) {
				
				Message msgReply = deleteM.read(sock, buffer);
				
				if(msgReply.getMessageCode().equals(MessageCode.OK)) 
				{	
					is_remove_ok.complete(true);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return is_remove_ok;
	}

	@Override
	public CompletableFuture<String> subscribe(Selector selector, Listener listener) {
		CompletableFuture<String> subid = new CompletableFuture<String>();
		try {	
			SocketChannel sock = YaksImpl.getChannel();
			
			Message subscribeM = new  MessageFactory().getMessage(MessageCode.SUB, null);

			subscribeM.add_property(Message.WSID, String.valueOf(wsid));

			subscribeM.setPath(Path.ofString(selector.toString()));
			
			subscribeM.write(sock, subscribeM);

			//read response msg
			ByteBuffer buffer = ByteBuffer.allocate(1);
			sock.read(buffer);
			Message msgReply = subscribeM.read(sock, buffer);
			
			if(msgReply.getMessageCode().equals(MessageCode.OK)) 
			{
				subid.complete(msgReply.getPropertiesList().get(Message.SUBID));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		return subid;
	}
	
	@Override
	public CompletableFuture<String> subscribe(Selector selector) {
		CompletableFuture<String> subid = new CompletableFuture<String>();
		try {	
			SocketChannel sock = YaksImpl.getChannel();
			
			Message subscribeM = new  MessageFactory().getMessage(MessageCode.SUB, null);

			subscribeM.add_property(Message.WSID, String.valueOf(wsid));

			subscribeM.setPath(Path.ofString(selector.toString()));
			
			subscribeM.write(sock, subscribeM);

			//read response msg
			ByteBuffer buffer = ByteBuffer.allocate(1);
			sock.read(buffer);
			Message msgReply = subscribeM.read(sock, buffer);
			
			if(msgReply.getMessageCode().equals(MessageCode.OK)) 
			{
				subid.complete(msgReply.getPropertiesList().get(Message.SUBID));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		return subid;
	}

	@Override
	public CompletableFuture<Boolean> unsubscribe(String subid) {
		CompletableFuture<Boolean> is_unsub_ok = new CompletableFuture<Boolean>();
		try {	
			SocketChannel sock = YaksImpl.getChannel();
			
			Message unsubM = new  MessageFactory().getMessage(MessageCode.UNSUB, null);

			unsubM.add_property(Message.WSID, String.valueOf(wsid));

			unsubM.setPath(Path.ofString(subid.toString()));
			
			unsubM.write(sock, unsubM);

			//read response msg
			ByteBuffer buffer = ByteBuffer.allocate(1);
			sock.read(buffer);
			Message msgReply = unsubM.read(sock, buffer);
			
			if(msgReply.getMessageCode().equals(MessageCode.OK)) 
			{
				is_unsub_ok.complete(true);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return is_unsub_ok;
	}

	@Override
	public CompletableFuture<Boolean> register_eval(Path path, Listener evcb) {
		CompletableFuture<Boolean> is_regeval_ok = new CompletableFuture<Boolean>();
		try {	
			SocketChannel sock = YaksImpl.getChannel();
			
			Message regEvalM = new  MessageFactory().getMessage(MessageCode.REG_EVAL, null);

			regEvalM.add_property(Message.WSID, String.valueOf(wsid));

			regEvalM.setPath(Path.ofString(path.toString()));
			
			regEvalM.write(sock, regEvalM);

			//read response msg
			ByteBuffer buffer = ByteBuffer.allocate(1);
			sock.read(buffer);
			Message msgReply = regEvalM.read(sock, buffer);
			if(msgReply.getMessageCode().equals(MessageCode.OK)) 
			{
				is_regeval_ok.complete(true);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return is_regeval_ok;
	}

	@Override
	public CompletableFuture<Boolean> unregister_eval(Path path) {
		
		CompletableFuture<Boolean> is_unregeval_ok = new CompletableFuture<Boolean>();
		try {	
			SocketChannel sock = YaksImpl.getChannel();

			Message unreg_evalM = new  MessageFactory().getMessage(MessageCode.UNREG_EVAL, null);

			unreg_evalM.add_property(Message.WSID, String.valueOf(wsid));

			unreg_evalM.setPath(Path.ofString(path.toString()));

			unreg_evalM.write(sock, unreg_evalM);


			//read response msg
			ByteBuffer buffer = ByteBuffer.allocate(1);
			sock.read(buffer);
			Message msgReply = unreg_evalM.read(sock, buffer);

			if(msgReply.getMessageCode().equals(MessageCode.OK)) 
			{
				is_unregeval_ok.complete(true);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return is_unregeval_ok;
	}

	@Override
	public CompletableFuture<String> eval(Selector selector) {
		CompletableFuture<String> values = null;
		try {	
			SocketChannel sock = YaksImpl.getChannel();

			Message evalM = new  MessageFactory().getMessage(MessageCode.EVAL, null);

			evalM.add_property(Message.WSID, String.valueOf(wsid));

			evalM.setSelector(selector);

			evalM.write(sock, evalM);

			//read response msg
			ByteBuffer buffer = ByteBuffer.allocate(1);
			sock.read(buffer);
			Message msgReply = evalM.read(sock, buffer);

			if(msgReply != null && msgReply.getMessageCode().equals(MessageCode.VALUES)) 
			{
				if(!msgReply.getValuesList().isEmpty()){
					
					for (Map.Entry<Path, Value> pair : ((Message) msgReply).getValuesList().entrySet()) {
						values.complete("(" + pair.getKey().toString() +", " +  pair.getValue().getValue().toString()+")");
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
	public CompletableFuture<Void> setWsid(int id) {
		wsid.complete(id);
		return null;
	}
	
	@Override
	public CompletableFuture<Integer> getWsid() {
		return wsid;
	}
}
