/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Tareq
 */
public class ConcurrentArrayList <T extends Comparable<T>> {
    private final ArrayList<T> m_list;
    private final Lock m_lock;
    /**
     * Constructor
     */
    public ConcurrentArrayList() {
            m_list = new ArrayList<>();
            m_lock = new ReentrantLock();
    }
    /**
     * Returns item from list
     * @param index Index of item in the list
     * @return Item of type T
     */
    public T get(int index) {
        T result =null;
        m_lock.lock();
        try {
            result = m_list.get(index);
        } finally {
            m_lock.unlock();
        }
        return result; 
    }
    /**
     * Adds an item to the list
     * @param item
     * @return 
     */
    public boolean add(T item) {
        boolean flage = false;
        m_lock.lock();
        try { 
            m_list.add(item);
            flage = true;
        } finally { 
            m_lock.unlock();
        }
        return flage;
    }
    
    /**
     * Removes an item from the list
     * @param item
     * @return
     */
    public boolean remove(T item) {
        boolean result = false;
        m_lock.lock();
        try {
            result = m_list.remove(item);
        } finally {
            m_lock.unlock();
        }
        return result;
    }
    public boolean remove(Object object) {
        boolean result = false;
        m_lock.lock();
        try {
            for(int i = 0; i < m_list.size(); i++) {
                    if(m_list.get(i).equals(object)) {
                            m_list.remove(i);
                            result = true;
                            break;
                    }
            }
        } finally {
            m_lock.unlock();
        }
        return result;
    }
    /**
     * Clear the list
     */    
    public void clear() {
        m_lock.lock();
        try {
            m_list.clear();     
        } finally {
            m_lock.unlock();
        }
    }
    /**
     * Returns the size of the list
     * @return Integer size of list
     */
    public int size() {
        int result = 0;
        m_lock.lock();
        try {
            result = m_list.size();
        } finally {
            m_lock.unlock();
        }
        return result;
    }

    /**
     * Sorts the list using a comparator
     * @param c
     */
    public void sort(Comparator<T> c) {
        m_lock.lock();
        try {
            Collections.sort(m_list, c);
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Sorts the list using the default comparer
     */
    public void sort() {
        m_lock.lock();
        try {
            Collections.sort(m_list);
        } finally {
            m_lock.unlock();
        }
    }
    /**
     * Locks the list to the current thread
     */
    public void lock() {
            m_lock.lock();
    }

    /**
     * Unlocks the list from the current thread
     */ 
    public void unlock() {
            m_lock.unlock();
    }
    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>The returned iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    public Iterator<T> iterator() {
        return m_list.iterator();
    }    
    
    public ArrayList<T> getList() {
        return m_list;
    }
    public boolean isEmpty() {
        return m_list.isEmpty();
    } 
    public void addAll(List<T> asList) {
        m_lock.lock();
        try {
            m_list.addAll(asList);
        } finally {
            m_lock.unlock();
        }    
    }    
    public T[] toArray(T[] array) {
        return m_list.toArray(array);
        
    }
}