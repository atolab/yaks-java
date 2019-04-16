package is.yaks.socket.messages;

import java.util.Properties;

import is.yaks.Message;
import is.yaks.utils.MessageCode;

public class MessageFactory {

	// use the getMessage method to get corresponding message type object

	public Message getMessage(MessageCode msgCode, Properties properties) 
	{
		if(msgCode.equals(MessageCode.LOGIN)) {
			return new LoginMessage(properties);
		} else if(msgCode.equals(MessageCode.LOGOUT)) {
			return new LogoutMessage();
		} else if(msgCode.equals(MessageCode.WORKSPACE)) {
			return new WorkspaceMessage();
		} else if(msgCode.equals(MessageCode.PUT)) {
			return new PutMessage(properties);
		} else if(msgCode.equals(MessageCode.GET)) {
			return new GetMessage();
		} else if(msgCode.equals(MessageCode.UPDATE)) {
			return new UpdateMessage();
		} else if(msgCode.equals(MessageCode.DELETE)) {
			return new DeleteMessage();
		} else if(msgCode.equals(MessageCode.SUB)) {
			return new SubscribeMessage();
		} else if(msgCode.equals(MessageCode.UNSUB)) {
			return new UnsubscribeMessage();
		} else if(msgCode.equals(MessageCode.NOTIFY)) {
			return new NotifyMessage();
		} else if(msgCode.equals(MessageCode.EVAL)) {
			return new EvalMessage();
		} else if(msgCode.equals(MessageCode.REG_EVAL)) {
			return new RegisterEvalMessage();
		} else if(msgCode.equals(MessageCode.UNREG_EVAL)) {
			return new UnregisterEvalMessage();
		} else if(msgCode.equals(MessageCode.VALUES)) {
			return new ValuesMessage();
		} else if(msgCode.equals(MessageCode.OK)) {
			return new OkMessage();
		} else if(msgCode.equals(MessageCode.ERROR)) {
			return new ErrorMessage();
		} 
		return null;
	}

}
