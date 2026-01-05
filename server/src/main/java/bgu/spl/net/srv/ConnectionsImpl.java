package bgu.spl.net.srv;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
public class ConnectionsImpl<T> implements Connections<T> {

    private HashMap<Integer,ConnectionHandler<T>> connecetions;
    private SubscriptionManager<T> subManager;
    private UniqueIDGenerator conIdGen;
    
    public ConnectionsImpl(){
        this.connecetions = new HashMap<>();
        this.conIdGen = new UniqueIDGenerator();
        this.subManager = new SubscriptionManager<>();
    }

    public void connect(ConnectionHandler<T> newClient)
    {
        this.connecetions.put(this.conIdGen.getNextId(),newClient);
    }

    public boolean send(int connectionId, T msg)
    {
        try{
            this.connecetions.get(connectionId).send(msg);
        }
        catch (Exception e){
            return false;
        }
        return true;
    }
    public void subscribe(String channel,int connectionId)
    {
        this.subManager.subscribe(channel,this.connecetions.get(connectionId),connectionId);
    }
    public void unsubscribe(int subId, int connectionId)
    {
        this.subManager.unsubscribe(subId,connectionId);
    }
    public void send(String channel, T msg)
    {
        this.subManager.broadcastChannel(channel, msg);
    }
    /*
    Deletes a connection from the dict using the connection ID.
    */
    public void disconnect(int connectionId){
        
        this.connecetions.remove(connectionId);
        this.conIdGen.freeId(connectionId);
        this.subManager.disconnectUser(connectionId);
    }
    
}
