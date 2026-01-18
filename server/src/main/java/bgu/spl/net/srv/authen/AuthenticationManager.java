package bgu.spl.net.srv.authen;
import bgu.spl.net.impl.data.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuthenticationManager {

    private ConcurrentHashMap<String, User> logins; 

    public AuthenticationManager()
    {
        logins = new ConcurrentHashMap<>();
    }

    public synchronized boolean isRegistered(String user)
    {
        return logins.containsKey(user);
    }

    public synchronized void register(String user,String pass,int connectionId)
    {
        if(isRegistered(user))
            throw new IllegalStateException("Client is already registered.");

        logins.put(user,new User(connectionId,user, pass));
        
        logins.get(user).login();

    }

    public synchronized boolean isConnected(String user)
    {
        if(!isRegistered(user))
            return false;

        return logins.get(user).isLoggedIn();
    }

    public synchronized void login(String user,String pass,int connectionId)
    {
        if(!isRegistered(user))
            throw new IllegalStateException("user is not registered.");

        User u = logins.get(user);
        if(u.isLoggedIn())
            throw new IllegalStateException("User is already logged in.");
        if(!u.cmpPassword(pass))
            throw new IllegalStateException("Wrong password.");

        u.login();
        u.setConnectionId(connectionId);
        
    }

    public synchronized void logout(String user)
    {
        if(!isRegistered(user))
            throw new IllegalStateException("user is not registered.");

        if(!isConnected(user))
            throw new IllegalStateException("user is not logged in.");

        logins.get(user).logout();
    }

    public User getUser(String userName)
    {
        return logins.get(userName);
    }   

}