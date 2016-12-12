/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classes;

import com.google.common.base.Joiner;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Tareq
 */
public class ConcurrentList<T extends Comparable<T>> extends ConcurrentArrayList<T> {

    private final Set<Integer> _this;
    private T lastKey;

    public ConcurrentList() {
        super();
        this._this =  Collections.synchronizedSet(new HashSet());
    }

    @Override
    public boolean add(T obj) {
        if (_this.add(obj.hashCode())) {
            lastKey = obj;
            return super.add(obj);
        }
        return false;
    }
    public boolean isKeyExist(T obj) {
        return _this.contains(obj.hashCode());
    }
        
    @Override
    public void clear(){
        super.clear();
        _this.clear();
    }

    public T getLastKey() {
        return lastKey;
    }

    @Override
    public void addAll(List<T> asList) {
        super.addAll(asList);
    }

    @Override
    public T[] toArray(T[] array) {
        return super.toArray(array);
    }
    public boolean contains(T item){
        boolean flag = false;
        Iterator<T> it = this.iterator();
        while(it.hasNext()){
            T t = it.next();
            if(t.equals(item)){
                flag = true;
                break;
            }
        }
        return flag;
    }
    @Override
    public String toString(){
        return Joiner.on(",").join(this.iterator());
        
    }
}

