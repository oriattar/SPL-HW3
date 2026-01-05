package bgu.spl.net.srv;

import java.util.ArrayList;
import java.util.List;

public class UniqueIDGenerator {
    
    private List<Integer> usedIds;

    public UniqueIDGenerator(){
        this.usedIds = new ArrayList<>();
    }

    /*
    A method that finds the smallest next unsued id and returns it.
    */
    public Integer getNextId(){
        Integer id = 0;

        while(this.usedIds.contains(id)) // generates the next available free id, the smallest available.
            id+=1;

        usedIds.add(id); //marks as used for now

        return id;
    }

    /*
    Frees entered ID in the generator, to allow future re-use.
    */
    public void freeId(Integer toFree){
        this.usedIds.remove(toFree);
    }
}
