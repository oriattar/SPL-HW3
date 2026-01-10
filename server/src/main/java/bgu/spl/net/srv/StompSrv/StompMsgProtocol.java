package bgu.spl.net.srv.StompSrv;

import java.util.HashMap;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class StompMsgProtocol implements StompMessagingProtocol<String> {

    private Connections<String> cManager;
    private int conId;
    private ConnectionHandler<String> ch;
    private boolean shouldTerminate = false;

    public void start(int connectionId, Connections<String> connections)
    {
        this.cManager = connections;
        this.conId = connectionId;
        if(ch == null)
            throw new IllegalStateException("Connection handler is not set");
        cManager.connect(ch,connectionId);
    }

    public void setConHandler(ConnectionHandler<String> ch)
    {
        if(ch == null)
            throw new IllegalArgumentException("Connection handler cannot be null");
        
        this.ch = ch;
    }

    public void process(String message)
    {
        String[] lines = message.split("\\R");
        String command = lines[0];
        HashMap<String,String> headers = new HashMap<>();

        int i =1;
        String line = lines[i];
        
        while(line != "\n" && line !="^@")
        {
            String [] temp = line.split(":");
            headers.put(temp[0],temp[1]);
            i+=1;
            line = lines[i];
        }

        try{
            switch (command) {
                case "CONNECT": 
                    solveConnect(headers);
                    break;
                case "SEND":
                    solveSend(headers, line);
                    break;
                case "SUBSCRIBE":
                    solveSubscribe(headers);
                    break;
                case "UNSUBSCRIBE":
                    solveUnsubscribe(headers);
                    break;
                case "DISCONNECT":
                    solveDisconnect(headers);
                    break;
                default://ERROR
                    throw new RuntimeException("Unknown command");
            }
        }
        catch (Exception e){
            solveError(e.getMessage());
        }

    }

    private void solveConnect(HashMap<String,String> headers)
    {
        cManager.send(conId, "CONNECTED\nversion:1.2\n\n^@");
    }
    
    private void solveSend(HashMap<String,String> headers,String body)
    {
       if(!headers.containsKey("destination"))
           throw new IllegalArgumentException("Did not contain a destination header, which is required for message propagation.");

       String channel = headers.get("destination");
       cManager.send(channel,body);
    }

    private void solveSubscribe(HashMap<String,String> headers)
    {
        if(!headers.containsKey("destination"))
           throw new IllegalArgumentException("Did not contain a destination header, which is required for subscription.");
        else if(!headers.containsKey("id"))
           throw new IllegalArgumentException("Did not contain an id header, which is required for subscription.");

        String channel = headers.get("destination");
        int subId = Integer.parseInt(headers.get("id"));
        cManager.subscribe(channel,conId,subId);
    }

    private void solveUnsubscribe(HashMap<String,String> headers)
    {
        if(!headers.containsKey("id"))
           throw new IllegalArgumentException("Did not contain an id header, which is required for unsubscription.");

        int subId = Integer.parseInt(headers.get("id"));
        cManager.unsubscribe(subId,conId);
    }

    private void solveDisconnect(HashMap<String,String> headers)
    {   
        if(headers.containsKey("receipt"))
        {
        int reciptId = Integer.parseInt(headers.get("receipt"));
        ///
        /// 
        }

        handleDisconnect();
    }

    private void solveError(String errorMsg)
    {
        String res = "ERROR\nmessage:"+errorMsg+"\n\n^@";
        cManager.send(conId, res);
        handleDisconnect();
    }

    private void handleDisconnect()
    {
        this.shouldTerminate = true;
         cManager.disconnect(conId);
    }
	/**
     * @return true if the connection should be terminated
     */
    public boolean shouldTerminate()
    {
        return shouldTerminate;
    }
    
}
