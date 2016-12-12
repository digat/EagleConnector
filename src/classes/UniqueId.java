/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classes;

/**
 *
 * @author Tareq
 */
public class UniqueId {
    private final ConcurrentList<Integer> vIds;

    public UniqueId() {
        vIds = new ConcurrentList<>();
    }
    public int getUniqueId(){
        int id=0;
        if(!vIds.isEmpty()){
            id = vIds.getLastKey()+1;
            vIds.add(id);
            return id;
        }else{
            vIds.add(id);
            return id;
        }
    }    
    
}

