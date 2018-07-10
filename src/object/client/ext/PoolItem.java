/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.client.ext;

import java.util.concurrent.atomic.AtomicReference;
import object.client.ext.States.ItemState;

/**
 *
 * @author madfooatcom
 */
public class PoolItem implements Comparable<PoolItem> {

    private final int id;
    private final AtomicReference<ItemState> itemState;
    private final ObjectClient objectClient;

    public PoolItem(int id, ObjectClient objectClient) {
        this.id = id;
        this.objectClient = objectClient;
        this.itemState = new AtomicReference(ItemState.notready);
        init();
    }

    private void init() {
        objectClient.addConnectioStateListener(evt -> {
            if (evt == States.ConnectioState.connected) {
                if (itemState.get() != ItemState.inuse) {
                    itemState.set(ItemState.free);
                }
            }else{
                itemState.set(ItemState.notready);
            }
        });
    }
    public void setItemState(ItemState is){
        itemState.set(is);
    }
    public ItemState getItemState(){
        return itemState.get();
    }

    public PoolItem(int id, ItemState itemState, ObjectClient objectClient) {
        this.id = id;
        this.itemState = new AtomicReference(ItemState.notready);
        this.objectClient = objectClient;
        init();
    }

    public int getId() {
        return id;
    }

    public ObjectClient getObjectClient() {
        return objectClient;
    }

    @Override
    public int compareTo(PoolItem t) {
        return Integer.compare(id, t.getId());
    }

}
