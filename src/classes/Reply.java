/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classes;

import classes.Enums.ReplyState;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Tareq
 */
public class Reply {
    private CountDownLatch latch = new CountDownLatch(1);
    private String message;
    private ReplyState replyState;

    public Reply() {
        replyState = ReplyState.START;
    }
    public Reply(String message) {
        this();
        this.message = message;
    }

    public boolean isReady() {
        return replyState==ReplyState.FINISH;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    public void resetLatch() {
        latch = new CountDownLatch(1);
    }       

    public ReplyState getReplyState() {
        return replyState;
    }
    public void waitForReply(int timeout, final TimeUnit tu) throws InterruptedException, TimeoutException {
        replyState=ReplyState.FINISH;
        if(!latch.await(timeout, tu)){
            //throw new TimeoutException();
        }
    }    
    public void unlock(){
        latch.countDown();
    }
}
