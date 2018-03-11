/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.client.ext;

import oms.Message;

/**
 *
 * @author madfooatcom
 */
public class TestClient {
    public static void main(String[] args) throws InterruptedException {
        ObjectClientConnectionPool ocp = new ObjectClientConnectionPool(1,"10.10.19.131", 7876, Message.class.getClassLoader());
        ocp.withMonitor();
        //ObjectClient oc = new ObjectClient("127.0.0.1", 7876, Message.class.getClassLoader());
        ocp.setup();
        Thread.sleep(2000);
        PoolItem pi = ocp.acquire();
        System.err.println(ocp.getFreeItemsCount());
        PoolItem pi1 = ocp.acquire();
        System.err.println(ocp.getFreeItemsCount());
        PoolItem pi2 = ocp.acquire();
        System.err.println(ocp.getFreeItemsCount());
    }
    
}
