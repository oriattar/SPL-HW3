package bgu.spl.net.srv.authen;
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

    public synchronized void register(String user,String pass)
    {
        if(isRegistered(user))
            throw new IllegalStateException("Client is already registered.");

        logins.put(user,new User(user, pass));
        
        logins.get(user).login(pass);

    }

    public synchronized boolean isConnected(String user)
    {
        if(!isRegistered(user))
            return false;

        return logins.get(user).isLoggedIn();
    }

    public synchronized void login(String user,String pass)
    {
        if(!isRegistered(user))
            throw new IllegalStateException("user is not registered.");

        logins.get(user).login(pass);
    }

    public synchronized void logout(String user)
    {
        if(!isRegistered(user))
            throw new IllegalStateException("user is not registered.");

        if(!isConnected(user))
            throw new IllegalStateException("user is not logged in.");

        logins.get(user).logout();
    }
}