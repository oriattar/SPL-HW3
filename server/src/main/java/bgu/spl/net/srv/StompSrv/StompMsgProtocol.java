package bgu.spl.net.srv.StompSrv;

import java.util.HashMap;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.authen.AuthenticationManager;

public class StompMsgProtocol implements StompMessagingProtocol<String> {

    private Connections<String> cManager;
    private int conId;
    private boolean shouldTerminate = false;
    private String userName = "";
    

    public void start(int connectionId, Connections<String> connections)
    {
        this.cManager = connections;
        this.conId = connectionId;
    }

    public void process(String message)
    {
        HashMap<String, String> headers = new HashMap<>();
        try{
            String[] lines = message.split("\\R"); // Split by new line characters
            if (lines.length == 0 || lines[0].isEmpty()) {
                throw new IllegalArgumentException("Empty message or missing command");
            }
            String command = lines[0];
            
            int i = 1;

            while (i < lines.length && !lines[i].isEmpty() && lines[i].charAt(0) != '\0') {
                String[] temp = lines[i].split(":", 2); // Split only on the first colon
                if (temp.length < 2) {
                    throw new IllegalArgumentException("Header line is incorrect format.");
                }
                headers.put(temp[0], temp[1]);
                i++;
            }

           String body = "";

            for (int j = i + 1; j < lines.length; j++) {
                body+=lines[j];
                if (j < lines.length - 1) {
                    body+="\n";
                }
            }
        
            switch (command) {
                case "CONNECT": 
                    solveConnect(headers);
                    if(headers.containsKey("receipt"))
                        sendRecipt(Integer.parseInt(headers.get("receipt")));
                    break;
                case "SEND":
                    conCheck();
                    solveSend(headers, body);
                    if(headers.containsKey("receipt"))
                        sendRecipt(Integer.parseInt(headers.get("receipt")));
                    break;
                case "SUBSCRIBE":
                    conCheck();
                    solveSubscribe(headers);
                    if(headers.containsKey("receipt"))
                        sendRecipt(Integer.parseInt(headers.get("receipt")));
                    break;
                case "UNSUBSCRIBE":
                    conCheck();
                    solveUnsubscribe(headers);
                    if(headers.containsKey("receipt"))
                        sendRecipt(Integer.parseInt(headers.get("receipt")));
                    break;
                case "DISCONNECT":
                    conCheck();
                    if(headers.containsKey("receipt"))
                        sendRecipt(Integer.parseInt(headers.get("receipt")));

                    handleDisconnect();
                    break;
                default://ERROR
                    throw new RuntimeException("Unknown command");
            }
            
        }
        catch (Exception e){
            System.out.println(e.getMessage() + " On Connection ID: " + conId);
            solveError(e.getMessage(),headers.get("receipt"));
        }

    }

    private void solveConnect(HashMap<String,String> headers)
    {
        if(!headers.containsKey("login") || !headers.containsKey("passcode"))
            throw new IllegalArgumentException("Did not contain login or passcode headers, which are required for connection.");

        String user = headers.get("login");
        String pass = headers.get("passcode");

        cManager.login(user, pass, conId);
        this.userName = user;
        cManager.send(conId, "CONNECTED\nversion:1.2\n\n\0");
    }
    
    private void solveSend(HashMap<String,String> headers,String body)
    {
       if(!headers.containsKey("destination"))
           throw new IllegalArgumentException("Did not contain a destination header, which is required for message propagation.");

       if(!cManager.isSubscribed(conId,headers.get("destination")))
           throw new IllegalArgumentException("Cant send message to a channel the user is not subscribed to.");
       
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

    private void solveError(String errorMsg,String receipt)
    {
        String res="";
        if(receipt != null)
            res = "ERROR\nreceipt-id: message-" + receipt + "\nmessage:"+ errorMsg+"\n\n\0";
        else
            res = "ERROR\nmessage:"+ errorMsg+"\n\n\0";
        cManager.send(conId, res);
        handleDisconnect();
    }

    private void handleDisconnect()
    {
        this.shouldTerminate = true;
        
        cManager.disconnect(conId);
        if(cManager.isUserLoggedIn(userName))
            cManager.logout(userName);
        userName = "";
    }
	/**
     * @return true if the connection should be terminated
     */
    public boolean shouldTerminate()
    {
        return shouldTerminate;
    }
    
    /*
    Method that checks if the handler is connected to the server.
    if not, throws an exception.
    */
    public void conCheck()
    {
        if(userName == "" || !cManager.isUserLoggedIn(userName))
            throw new IllegalStateException("Client is not logged in to the server.");
    }

    public void sendRecipt(int reciptId)
    {
        String res = "RECEIPT\nreceipt-id:"+reciptId+"\n\n\0";
        cManager.send(conId, res);
    }
}
