/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.client.ext;

import com.google.common.collect.Maps;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import object.client.ext.States.ConnectioState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author madfooatcom
 */
public class ObjectClientConnectionPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectClientConnectionPool.class);
    private final int poolSize;
    private final String remotehost;
    private final int port;
    private final ClassLoader classLoader;
    private final Map<Integer, PoolItem> pool;
    private final AtomicInteger usedItems = new AtomicInteger();
    private final AtomicInteger freeItems = new AtomicInteger();
    private final AtomicInteger createdItems = new AtomicInteger();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public ObjectClientConnectionPool(int poolSize, String remotehost, int port, ClassLoader classLoader) {
        this.poolSize = poolSize;
        this.remotehost = remotehost;
        this.port = port;
        this.classLoader = classLoader;
        pool = Maps.newConcurrentMap();
        
    }
    public void withMonitor(){
        setupMonitor();
    }
    private String poolStatistics(){
        //- Pool stats (total=50, active=0, idle=50, waiting=0)
        int total = pool.size();
        long active = pool.entrySet().stream().filter(item -> item.getValue().getItemState() == States.ItemState.inuse).count();
        Long free = pool.entrySet().stream().filter(item -> item.getValue().getItemState() == States.ItemState.free).count();
        long notReady = pool.entrySet().stream().filter(item -> item.getValue().getItemState() == States.ItemState.notready).count();
        long connected = pool.entrySet().stream().filter(item -> item.getValue().getObjectClient().getConnectioState() == ConnectioState.connected).count();
        StringBuilder sb = new StringBuilder();
        sb.append("- Pool stats ");
        sb.append("(total=").append(total).append(", ");
        sb.append("active=").append(active).append(", ");
        sb.append("free=").append(free).append(", ");
        sb.append("notReady=").append(notReady).append(", ");
        sb.append("connected=").append(connected).append("} ").append( new Date());
        freeItems.set(free.intValue());
        return sb.toString();
    }

    private void setupMonitor() {
        Runnable task = () -> {
            LOGGER.info("Statistics: " + poolStatistics());
            //System.err.println("Statistics: " + poolStatistics());
        };
        executor.scheduleWithFixedDelay(task, 0, 10, TimeUnit.SECONDS);
    }

    public void setup() {
        for (int i = 0; i < poolSize; i++) {
            pool.put(i, genrateItem(i));
            createdItems.getAndIncrement();
        }
        usedItems.set(0);
    }

    private PoolItem genrateItem(int id) {
        ObjectClient oc = new ObjectClient(id, remotehost, port, classLoader);
        oc.setup();
        oc.start();
        PoolItem pi = new PoolItem(id, oc);
        pi.setItemState(States.ItemState.free);
        return pi;
    }
    public PoolItem getPoolItem() {
        Random rn = new Random();
        int randomNum =  rn.nextInt(pool.size());
        PoolItem item = pool.get(randomNum);
        if(item!=null){
            return item;
        }else{
            return getPoolItem();
        }
    }
    /*
    public PoolItem acquire() {
        List<Entry<Integer, PoolItem>> list = pool.entrySet().stream().filter(item -> item.getValue().getItemState() == States.ItemState.free
                && item.getValue().getObjectClient().getConnectioState() == ConnectioState.connected)
                .collect(Collectors.toList());
        if (list != null) {
            if(!list.isEmpty()){
            Entry<Integer, PoolItem> entry = list.get(new Random().nextInt(list.size()));
            //companyName.get(new Random().nextInt(companyName.size()));
            if (entry != null) {
                entry.getValue().setItemState(States.ItemState.inuse);
                if(freeItems.get()>0)
                    freeItems.getAndDecrement();
                usedItems.getAndIncrement();
                return entry.getValue();
            } else {
                return null;
            }
            }else{
                return null;
            }
        } else {
            return null;
        }
    }

    public void release(PoolItem pi) {
        pi.setItemState(States.ItemState.free);
        pool.put(pi.getId(), pi);
        usedItems.getAndDecrement();
        freeItems.getAndIncrement();
    }

    public int getFreeItemsCount() {
        return freeItems.get();
    }
    
    public int getCreatedItems(){
        return createdItems.get();
    }

    public int getUsedItemsCount() {
        return usedItems.get();
    }
    */

}
