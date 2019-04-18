package is.yaks.socket.async;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import is.yaks.Message;
import is.yaks.Path;
import is.yaks.async.Yaks;
import is.yaks.socket.utils.VLEEncoder;

public class YaksRuntime {

	private YaksRuntime yaks_rt;

	public static int DEFAULT_TIMEOUT = 5;

	Map<VLEEncoder, Message> workingMap = new HashMap<VLEEncoder, Message>();
	Map<String, Message> listenersMap = new HashMap<String, Message>();
	Map<Path, Message> evalsMap = new HashMap<Path, Message>();

	int max_buffer_size = (64 * 1024);

	int max_buffer_count = 32;

	private static YaksRuntime instance;

	Runtime rt = null;

	private YaksRuntime(){
	}

	public static synchronized YaksRuntime getInstance() 
	{
		if( instance == null ) 
		{
			instance = new YaksRuntime();
		}
		return instance;
	}

	
	public boolean post_message(SocketChannel sock, Message msg) {

		return true;
	}

	public boolean read_message(YaksRuntime yaks_rt) {

		return true;
	}


	public CompletableFuture<Message> read(State driver) {

		
		
		return null;
	}
	
	// it creates the state called "driver" and return it
	public State create(Properties properties) {

		State driver = new State();

		int port;
		String h = (String)properties.get("host");
		String p = (String)properties.get("port");
		if(p.equals("")) {
			port = Yaks.DEFAUL_PORT;
		} else {
			port = Integer.parseInt(p);
		}
		try {

			InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(h), port);
			Selector selector = Selector.open();
			SocketChannel sock = SocketChannel.open(addr);
			sock.setOption(StandardSocketOptions.TCP_NODELAY, true);
			sock.configureBlocking(false);
			sock.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			SelectionKey key = null;
			
			driver = driver.create(sock);
			
			loop(driver);
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return driver;
	}
	

	public void receiver_loop(State driver) {
		try {

			CompletableFuture<Message> readFuture = read(driver);

			Message msgRead = readFuture.get();
			
			int vle = 0;
			while(vle == 0) {
				vle = VLEEncoder.read_vle(driver.getSock());
				Thread.sleep(YaksImpl.TIMEOUT);
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		/**
		1. 
		 * */

		//==> here it goes

		/*
let open Apero in
  let open Apero.Infix in
  MVar.read driver >>= fun self ->
  let%lwt len = Net.read_vle self.sock >>= Vle.to_int %> Lwt.return in
  let%lwt _ = Logs_lwt.debug (fun m -> m "Message lenght : %d" len) in
  let buf = Abuf.create len in
  let%lwt n = Net.read_all self.sock buf len in
  let () = check_socket self.sock in
  let%lwt _ = Logs_lwt.debug (fun m -> m "Read %d bytes out of the socket" n) in
  (try decode_message buf
  with e ->  Logs.err (fun m -> m "Failed in parsing message %s" (Printexc.to_string e)) ; raise e) |> fun msg ->
  match (msg.header.mid, msg.body) with
  | (NOTIFY, YNotification (subid, data)) ->
    MVar.read driver >>= fun self ->
    (match ListenersMap.find_opt subid self.subscribers with
    | Some cb ->
      (* Run listener's callback in future (catching exceptions) *)
      let _ =  Lwt.try_bind (fun () -> let%lwt _ = Logs_lwt.debug (fun m -> m "Notify received. Call listener for subscription %s" subid) in cb data)
        (fun () -> Lwt.return_unit)
        (fun ex -> let%lwt _ = Logs_lwt.warn (fun m -> m "Listener's callback of subscription %s raised an exception: %s\n %s" subid (Printexc.to_string ex) (Printexc.get_backtrace ())) in Lwt.return_unit)
      in
      (* Return unit immediatly to release socket reading thread *)
      Lwt.return_unit
    | None ->  
      let%lwt _ = Logs_lwt.debug (fun m -> m "Received notification with unknown subscriberid %s" subid) in
      Lwt.return_unit)

  | (EVAL, YSelector s) ->
    (* Process eval in future (catching exceptions) *)
    let _ = Lwt.try_bind
      (fun () -> process_eval s driver >>= fun results ->
      make_values msg.header.corr_id results >>= fun rmsg ->
      send_to_socket rmsg self.buffer_pool self.sock)
        (fun () -> Lwt.return_unit)
        (fun ex -> let%lwt _ = Logs_lwt.warn (fun m -> m "Eval's callback raised an exception: %s\n %s" (Printexc.to_string ex) (Printexc.get_backtrace ())) in
          make_error msg.header.corr_id INTERNAL_SERVER_ERROR >>= fun rmsg ->
          send_to_socket rmsg self.buffer_pool self.sock)
    in
    (* Return unit immediatly to release socket reading thread *)
    Lwt.return_unit

  | (_, _) -> MVar.guarded driver @@ (fun self ->
    (match WorkingMap.find_opt msg.header.corr_id self.working_set with 
    | Some (resolver, msg_list) ->
      let msg_list = List.append msg_list [msg] in
      if (Yaks_fe_sock_types.has_incomplete_flag msg.header.flags) then
        MVar.return () {self with working_set = WorkingMap.add msg.header.corr_id (resolver, msg_list) self.working_set}
      else
        let _ = Lwt.wakeup_later resolver msg_list in
        MVar.return () {self with working_set = WorkingMap.remove msg.header.corr_id self.working_set}
    | None -> let%lwt _ = Logs_lwt.warn (fun m -> m "Received message with unknown correlation id %Ld" msg.header.corr_id) in
      MVar.return () self)
    )
		 * */
		return;
	}

	public void add_listener() {

	}

	public void remove_listener() {

	}

	public void add_eval_callback() {

	}

	
	public void remove_eval_callback() {


	}

	public void notify_listeners() {

	}

	public void execute_eval() {

	}

	public void handle_reply() {

	}

	public void handle_unexpected_message() {

	}

	public void close() {

	}

	//equivalent to loop function in ocaml's file ocaml_sock_driver.ml
	public void loop(State driver) {
		receiver_loop(driver);
	}

}
