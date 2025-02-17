package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ConnectionsImpl <T> implements Connections<T> {
    private HashMap<Integer,ConnectionHandler<T>> connectionHandlers;
    private HashMap<Integer,String> usernames;


    private List<Integer> needToDisconnect;

    public ConnectionsImpl(){
        connectionHandlers = new HashMap<>();
        usernames = new HashMap<>();
        needToDisconnect = new LinkedList<>();
    }


    //add a client connectionId to active client map.
    public boolean connect(int connectionId, ConnectionHandler<T> handler){
        if (connectionHandlers.containsKey(connectionId))
            return false;
        connectionHandlers.put(connectionId,handler);
        usernames.put(connectionId,"");
        return true;
    }
    //sends a message T to the client represented by the given connectionId
    @Override
    public boolean send(int connectionId, T msg) {
        if (!connectionHandlers.containsKey(connectionId))
            return false;
        ConnectionHandler<T> handler = connectionHandlers.get(connectionId);
        handler.send(msg);
        if (needToDisconnect.contains(connectionId)){
            needToDisconnect.remove((Integer) connectionId);
            usernames.remove(connectionId);
            connectionHandlers.remove(connectionId);
            //TODO:: delete socket?
        }
        return true;
    }

    @Override
    public void send(String channel, T msg) {

    }
    //Removes an active client connectionId from the map
    @Override
    public void disconnect(int connectionId) {
       //just remove? maybe delete the socket? how to delete the socket?
        needToDisconnect.add(connectionId);

    }
    public void broadcast(T msg){
        for (Integer clientId : usernames.keySet()) {
            if(!usernames.get(clientId).equals("")){
                this.send(clientId,msg);
            }
        }

    }
    public boolean checkIfUserNameExist(String username)
    {
        return this.usernames.containsValue(username);
    }
    public boolean checkIfClientLoggedIn(int clientId)
    {
        return !this.usernames.get(clientId).equals("");
    }
    public void addUsername(int clientId,String username){
        this.usernames.put(clientId,username);
    }


}
