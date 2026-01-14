package bgu.spl.net.srv.authen;

public class User {
    
    private String userName;
    private String password;
    private boolean isLoggedIn;

    public User(String userName, String password)
    {
        this.userName = userName;
        this.password = password;
        this.isLoggedIn = false;

    }

    public String getUserName() {
        return userName;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void login(String password)
    {
        if(!this.password.equals(password))
            throw new IllegalArgumentException("Password is incorrect.");

        if(isLoggedIn)
            throw new IllegalStateException("User is already logged in.");

        isLoggedIn = true;
    }

    public void logout()
    {
        if(!isLoggedIn)
            throw new IllegalStateException("User is not logged in.");

        isLoggedIn = false;
    }
}