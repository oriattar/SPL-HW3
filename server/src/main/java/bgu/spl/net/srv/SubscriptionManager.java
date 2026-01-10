package bgu.spl.net.srv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class SubscriptionManager<T>
{
    /*
    The subscriptionManager class, Handles subscrition maintance and allowes message sending to a subscriptions.
    
    */
    public class Subscription {

        /*
        Subclass that represents an active subscription.
        Saves subscription id
        ConnectionHandler object to enable client communication.
        Name of the subscription channel.
        */
        private int subId;
        private ConnectionHandler<T> client;
        private String channel;
        
        /*
        Constructs a subscription
        */
        public Subscription(int subId,ConnectionHandler<T> con,String channel)
        {
            this.channel = channel;
            this.subId =subId;
            this.client = con;
        }

        public int getId(){return this.subId;}

        public String getChannel(){return this.channel;}

        /*
        Handles message sending via subscription
        */
        public void send(T message){
            client.send(message);
        }
    };
    private ConcurrentHashMap<String,List<Subscription>> subscriptions; // Saves the subscriptions by their channel name.
    private ConcurrentHashMap<Integer,List<Subscription>> subDict; // Saves the subscriptions by ConnectionID.
    private UniqueIDGenerator mesIdGen;


    /*
    Construct the sub-manager.
    */
    public SubscriptionManager()
    {
        this.subscriptions = new ConcurrentHashMap<>();
        this.subDict = new ConcurrentHashMap<>();
        this.mesIdGen = new UniqueIDGenerator();
    }

    public Subscription createSubscription(int subId,ConnectionHandler<T> con,String channel)
    {
        return new Subscription(subId,con,channel);
    }

    /*
    Method that handles creating a new list of subscriptions if a new topic was inserted.
    */
    public void addTopicIfNeeded(String channel){
        if(!this.subscriptions.containsKey(channel))
            this.subscriptions.put(channel, new ArrayList<>());
    }

    /*
    Removes a topic from the dict when there are no subscriptoins to it.
    */
    public void removeTopicIfNeeded(String channel){
        if(this.subscriptions.containsKey(channel) && this.subscriptions.get(channel).size()==0)
            this.subscriptions.remove(channel);
    }


    /*
    Handles user subscription to a channel.
    */
    public void subscribe(Subscription s,int subId,int conId){

        addTopicIfNeeded(s.getChannel()); // adds topic

        this.subscriptions.get(s.getChannel()).add(s);
        if(!this.subDict.containsKey(conId)) //register in the dict if the user is new.
        {
            this.subDict.put(conId,new ArrayList<>());
        }
        this.subDict.get(conId).add(s); //adds to the user dict.
    }


    /*
    
    */
    public void unsubscribe(int subscriptionId,int connectionId)
    {
        if(!subDict.containsKey(connectionId))
            throw new RuntimeException("Cannot cancel subscription on a non existing user.");
        
        List<Subscription> list = this.subDict.get(connectionId);
        for(int i=0;i<list.size();i=i+1)
        {
            Subscription s = list.get(i);
            if(s.getId() == subscriptionId)
            {
                removeUser(s);
                return;
            }
        }
    }

    /*
    Method that broadcasts a message to each subscribed user on the channel.
    */
    public void broadcastChannel(String channel,T message){
        if(!this.subscriptions.containsKey(channel))
            throw new RuntimeException("Cannot broadcast a message on a non existing topic.");

        List<Subscription> list = this.subscriptions.get(channel); //going over the list
        for(Subscription s:list)
        {
            String res = "MESSAGE\nsubscription:"+s.getId() + "\nmessage-id:"+mesIdGen.getNextId()+"\ndestination:"+channel+"\n\n"+message+"\n^@";
            s.send((T)res); //sends mes
        }
    }

    /*
    Method that handles unsubscribing from all the channels that the user is subscribed to.
    */
    public void disconnectUser(int connectionId)
    {
        if(!this.subDict.containsKey(connectionId))
            throw new RuntimeException("Cannot remove an unregistered user from the manager");

        List<Subscription> list = this.subDict.get(connectionId);
        for(Subscription s:list) // going over user's subscription
        {
            removeUser(s);
        }
        this.subDict.remove(connectionId); // remove user entry
    }

    /*
    Helper method that removes a user from the channel mapping.
    */
    private void removeUser(Subscription s)
    {
        this.subscriptions.get(s.getChannel()).remove(s); //removes it from the list (by channel)
        this.removeTopicIfNeeded(s.getChannel());
    }

}
