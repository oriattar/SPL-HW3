package bgu.spl.net.srv;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
public class ConnectionsManager<T> implements Connections<T> {

    /*
    Class that helps saving and maintaining all active connections.
    Maps a connection handler to a connection ID.
    Manages subscription, via subscription manager object.
    
    */
    private ConcurrentHashMap<Integer,ConnectionHandler<T>> connecetions;
    private SubscriptionManager<T> subManager;
   
    
    /*
    Connection manager constructor, constructs the above fields.
    */
    public ConnectionsManager(){
        this.connecetions = new ConcurrentHashMap<>();
        this.subManager = new SubscriptionManager<>();
    }

    /*
    Method that handles registering new connection.
    */
    public void connect(ConnectionHandler<T> newClient,int conId)
    {
        this.connecetions.put(conId,newClient); // insert a new entry in the dict, with a new generated id
    }

    /*
    Method that handles sending message to the client, via its CH.
    */
    public boolean send(int connectionId, T msg)
    {
        try{
            this.connecetions.get(connectionId).send(msg); //gets the CH via its id and sends the message.
        }
        catch (Exception e){
            return false;
        }
        return true;
    }

    /*
    Method that handles a client desired subscription, sends to the subscription manager.
    */
    public void subscribe(String channel,int connectionId,int subId)
    {
        SubscriptionManager<T>.Subscription sub = this.subManager.createSubscription(subId,this.connecetions.get(connectionId),channel);
        this.subManager.subscribe(sub,subId,connectionId);
    }

    /*
    Method that handles unsubscribing via the subscruptoin manager.
    */
    public void unsubscribe(int subId, int connectionId)
    {
        this.subManager.unsubscribe(subId,connectionId);
    }

    /*
    Method that sends the message to sub manager to broadcast to the desired channel.
    */
    public void send(String channel, T msg)
    {
        this.subManager.broadcastChannel(channel, msg);
    }
    /*
    Deletes a connection from the dict using the connection ID.
    */
    public void disconnect(int connectionId){
        
        this.connecetions.remove(connectionId);
        this.subManager.disconnectUser(connectionId);
    }
    
}
