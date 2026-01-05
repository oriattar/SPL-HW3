package bgu.spl.net.srv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscriptionManager<T>
{
    
    private class Subscription {

        private int subId;
        private ConnectionHandler<T> client;
        private String channel;
        
        public Subscription(int subId,ConnectionHandler<T> con,String channel)
        {
            this.channel = channel;
            this.subId =subId;
            this.client = con;
        }

        public int getId(){return this.subId;}

        public String getChannel(){return this.channel;}

        public void send(T message){
            client.send(message);
        }
    };
    private HashMap<String,List<Subscription>> subscriptions;
    private HashMap<Integer,List<Subscription>> subDict;
    private UniqueIDGenerator subIdGen;

    public SubscriptionManager()
    {
        this.subscriptions = new HashMap<>();
        this.subIdGen = new UniqueIDGenerator();
        this.subDict = new HashMap<>();
    }

    public void addTopicIfNeeded(String channel){
        if(!this.subscriptions.containsKey(channel))
            this.subscriptions.put(channel, new ArrayList<>());
    }
    public void removeTopicIfNeeded(String channel){
        if(this.subscriptions.containsKey(channel) && this.subscriptions.get(channel).size()==0)
            this.subscriptions.remove(channel);
    }

    public void subscribe(String channel,ConnectionHandler<T> con,int conId){
        Integer id = this.subIdGen.getNextId();
        Subscription toAdd = new Subscription(id,con,channel);

        addTopicIfNeeded(channel);

        this.subscriptions.get(channel).add(toAdd);
        if(!this.subDict.containsKey(conId))
        {
            this.subDict.put(conId,new ArrayList<>());
        }
        this.subDict.get(conId).add(toAdd);
    }

    public void unsubscribe(int subscriptionId,int connectionId)
    {
        if(!subDict.containsKey(connectionId))
            throw new RuntimeException("Cannot cancel subscription on a non existing user.");
        for(String chan : this.subscriptions.keySet())
        {
            List<Subscription> list = this.subscriptions.get(chan);
            for(int i=0;i<list.size();i=i+1)
            {
                if(subscriptionId == list.get(i).getId())
                {
                    Subscription toRemove = list.remove(i);
                    this.subIdGen.freeId(subscriptionId);
                    this.subDict.get(connectionId).remove(toRemove);
                    removeTopicIfNeeded(chan);
                    return;
                }
            }
        }
    }

    public void broadcastChannel(String channel,T message){
        if(!this.subscriptions.containsKey(channel))
            throw new RuntimeException("Cannot broadcast a message on a non existing topic.");

        List<Subscription> list = this.subscriptions.get(channel);
        for(Subscription s:list)
        {
            s.send(message);
        }
    }

    public void disconnectUser(int connectionId)
    {
        if(!this.subDict.containsKey(connectionId))
            throw new RuntimeException("Cannot remove an unregistered user from the manager");

        List<Subscription> list = this.subDict.get(connectionId);
        for(Subscription s:list)
        {
            this.subscriptions.get(s.getChannel()).remove(s);
            this.subIdGen.freeId(s.getId());
        }
        this.subDict.remove(connectionId);
    }


}
