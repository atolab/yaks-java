package is.yaks.socket.async;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import is.yaks.Path;
import is.yaks.socket.lib.Message;
import is.yaks.socket.utils.ByteBufferPoolImpl;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.utils.ByteBufferPool;

public class State {

    SocketChannel sock;
    Map<VLEEncoder, Message> workingMap;
    Map<String, Message> listenersMap;
    Map<Path, Message> evalsMap;
    ByteBufferPool buffer_pool;

    // the state is called driver in ocaml's file yaks_sock_driver.ml
    public State create(SocketChannel sc) {
        this.sock = sc;
        this.workingMap = new HashMap<VLEEncoder, Message>();
        this.listenersMap = new HashMap<String, Message>();
        this.evalsMap = new HashMap<Path, Message>();
        this.buffer_pool = new ByteBufferPoolImpl();
        return this;
    }

    public SocketChannel getSock() {
        return sock;
    }

    public void setSock(SocketChannel sock) {
        this.sock = sock;
    }

    public Map<VLEEncoder, Message> getWorkingMap() {
        return workingMap;
    }

    public void setWorkingMap(Map<VLEEncoder, Message> workingMap) {
        this.workingMap = workingMap;
    }

    public Map<String, Message> getListenersMap() {
        return listenersMap;
    }

    public void setListenersMap(Map<String, Message> listenersMap) {
        this.listenersMap = listenersMap;
    }

    public Map<Path, Message> getEvalsMap() {
        return evalsMap;
    }

    public void setEvalsMap(Map<Path, Message> evalsMap) {
        this.evalsMap = evalsMap;
    }

    public ByteBufferPool getBuffer_pool() {
        return buffer_pool;
    }

    public void setBuffer_pool(ByteBufferPool buffer_pool) {
        this.buffer_pool = buffer_pool;
    }

}
