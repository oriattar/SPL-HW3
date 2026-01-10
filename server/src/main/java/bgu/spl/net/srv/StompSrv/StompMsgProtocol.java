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
        try{
            String[] lines = message.split("\\R"); // Split by new line characters
            if (lines.length == 0 || lines[0].isEmpty()) {
                throw new IllegalArgumentException("Empty message or missing command");
            }
            String command = lines[0];
            HashMap<String, String> headers = new HashMap<>();
            int i = 1;

            while (i < lines.length && !lines[i].isEmpty() && lines[i].charAt(0) != '\0') {
                String[] temp = lines[i].split(":", 2); // Split only on the first colon
                if (temp.length < 2) {
                    throw new IllegalArgumentException("Header line is incorrect format.");
                }
                headers.put(temp[0], temp[1]);
                i++;
            }
           StringBuilder bodyBuilder = new StringBuilder();

            for (int j = i + 1; j < lines.length; j++) {
                bodyBuilder.append(lines[j]);
                if (j < lines.length - 1) {
                    bodyBuilder.append("\n");
                }
            }

            String body = bodyBuilder.toString();
        
            switch (command) {
                case "CONNECT": 
                    solveConnect(headers);
                    break;
                case "SEND":
                    solveSend(headers, body);
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
            System.out.println(e.getMessage() + " On Connection ID: " + conId);
            solveError(e.getMessage());
        }

    }

    private void solveConnect(HashMap<String,String> headers)
    {
        System.out.println("User connected with id: " + conId);
        cManager.send(conId, "CONNECTED\nversion:1.2\n\n\0");
    }
    
    private void solveSend(HashMap<String,String> headers,String body)
    {
       if(!headers.containsKey("destination"))
           throw new IllegalArgumentException("Did not contain a destination header, which is required for message propagation.");

       System.out.println("Sending message from user id: " + conId + " to channel: " + headers.get("destination"));
       String channel = headers.get("destination");
       cManager.send(channel,body + "\0");
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

        System.out.println("User with id: " + conId + " subscribed to channel: " + channel + " with subscription id: " + subId);
    }

    private void solveUnsubscribe(HashMap<String,String> headers)
    {
        if(!headers.containsKey("id"))
           throw new IllegalArgumentException("Did not contain an id header, which is required for unsubscription.");

        int subId = Integer.parseInt(headers.get("id"));
        cManager.unsubscribe(subId,conId);
        System.out.println("User with id: " + conId + " unsubscribed from subscription id: " + subId);
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
        String res = "ERROR\nmessage:"+errorMsg+"\n\n\0";
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
