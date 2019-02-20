package is.yaks.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import is.yaks.YaksRuntime;
import is.yaks.socketfe.Message;
import is.yaks.socketfe.MessageCode;

public class YaskRuntimeImpl implements YaksRuntime {

	@Override
	public void close() {
		
	}

	@Override
	public boolean post_message(SocketChannel sock, Message putM) {
		boolean is_put_ok =false;
		
		putM.write(sock, putM);
		
		ByteBuffer buffer = ByteBuffer.allocate(1);
		try {
			sock.read(buffer);

			Message msgReply = putM.read(sock, buffer);
			if(msgReply.getMessageCode().equals(MessageCode.OK)) 
			{
				is_put_ok = true;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return is_put_ok;

	}

	@Override
	public void add_listener() {
		
	}

	@Override
	public void remove_listener() {
		
	}

	@Override
	public void add_eval_callback() {
		
	}

	@Override
	public void remove_eval_callback() {
		
	}

	@Override
	public void notify_listeners() {
		
	}

	@Override
	public void execute_eval() {
		
	}

	@Override
	public void handle_reply() {
		
	}

	@Override
	public void handle_unexpected_message() {
		
	}

	@Override
	public void run() {
		
	}
	
	

}
