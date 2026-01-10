package bgu.spl.net.srv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UniqueIDGenerator {
    
    AtomicInteger nextId;

    public UniqueIDGenerator(){
        this.nextId = new AtomicInteger(0);
    }

    /*
    A method that finds the smallest next unsued id and returns it.
    */
    public Integer getNextId(){
        return nextId.getAndIncrement();
    }
}

   
