package loom;

import java.lang.InterruptedException;
import java.lang.RuntimeException;
import java.lang.Thread;

/**
 * Thread Utilities used by Emulating Continuation.
 *
 * @author Øystein Myhre Andersen
 *
 */
public class ThreadUtils {
    private static final boolean DEBUG = false;//true;
    private static final boolean TRACE = false;//true;
	public static RuntimeException _PENDING_EXCEPTION=null;

    
	// *********************************************************************
	// *** BASIC PRIMITIVE: _STARTTHREAD
	// *********************************************************************
    static void _STARTTHREAD(final Thread next) {
        Thread current=Thread.currentThread();
        if(TRACE) System.out.println("Continuation._STARTTHREAD: "+current.getName()+" ==> "+next.getName());
        prevThread=current;
        next.start();				// Start   'next'
        synchronized (current) {	// Suspend 'prev'
        	try { current.wait(); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    	waitUntilPrevThreadNotRunning();
        wakeupThread(next); // Continue here after suspend
    }
    
	// *********************************************************************
	// *** BASIC PRIMITIVE: SWAP_THREAD
	// *********************************************************************
    static void SWAP_THREAD(final Thread next) {
        Thread current=Thread.currentThread();
        if(TRACE) System.out.println("Continuation.SWAP_THREAD: "+current.getName()+" ==> "+next.getName());
        prevThread=current;
        assert(next!=current);
        synchronized (next) { next.notify(); }	// Resume  'next'
        synchronized (current) {				// Suspend 'prev'
        	try { current.wait(); } catch (InterruptedException ex) { ex.printStackTrace(); }
        }
    	waitUntilPrevThreadNotRunning();
        wakeupThread(next); // Continue here after suspend
    }
    
	// *********************************************************************
	// *** BASIC PRIMITIVE: END_THREAD
	// *********************************************************************
	static void END_THREAD(final Thread next) { 
        Thread current=Thread.currentThread();
        if(TRACE) System.out.println("Continuation.END_THREAD: "+current.getName()+" ==> "+next.getName());
        prevThread=current;
		synchronized (next) { next.notify(); } // Resume 'next'
    }
    
	// *********************************************************************
	// *** THREAD: waitUntilPrevThreadNotRunning
	// *********************************************************************
    private static Thread prevThread;
    private static void waitUntilPrevThreadNotRunning() {
        if(prevThread!=null) {
        	int count=10; // To prevent DeadLock
        	while((count--)>0 && prevThread.getState()==Thread.State.RUNNABLE) {
        		try { Thread.sleep(0,1); } catch (InterruptedException e) { e.printStackTrace(); }
        	}
        	prevThread=null;
        }
    }

	// *********************************************************************
	// *** THREAD: wakeupThread
	// *********************************************************************
	private static void wakeupThread(final Thread next) {
		Thread curr=Thread.currentThread();
		if (TRACE) System.out.println("Continuation.wakeupThread: WAKEUP "+curr);
		if (_PENDING_EXCEPTION != null) {
			RuntimeException t = _PENDING_EXCEPTION;
			_PENDING_EXCEPTION = null;
			if (TRACE) System.out.println("Continuation.wakeupThread: Re-throw " + t);
			if(t!=null) throw (t);
		}
		if(DEBUG) {
			if (TRACE)	System.out.println("Continuation.wakeupThread: END " + curr);
			assert(curr.getState()==Thread.State.RUNNABLE);
			while(next.getState()==Thread.State.RUNNABLE) Thread.yield();  
			assert(next.getState()!=Thread.State.RUNNABLE);
		}
	}

}
