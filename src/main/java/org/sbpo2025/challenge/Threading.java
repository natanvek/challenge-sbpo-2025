package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicIntegerArray;


final public class Threading {

private Threading() {};

public interface Task {
	void run();
}

static boolean aquire(Semaphore semaphore, int amount) {
	try {
		semaphore.acquire(amount);
	} catch (InterruptedException e) {
		System.out.println("CaughtInterruptError");
		return false;
	}
	return true;
}

static class Worker extends Thread {
	Pool manager;
	int workerId;
	boolean killThread = false;
	Task taskToRun = null;
	Semaphore waitUntilTaskSemaphore = new Semaphore(0);
	
	Worker(Pool manager, int workerId) {
		this.manager  = manager;
		this.workerId = workerId;
		this.start();
	}

	public void run() {
		while (!killThread) {
			if (!aquire(waitUntilTaskSemaphore, 1)) {
				manager.done(this.workerId, false);
				return;
			}
			if (this.taskToRun != null) {
				this.taskToRun.run();
			}
			manager.done(this.workerId, true);
		}
	}

	public void kill() {
		this.taskToRun = null;
		this.killThread = true;
		waitUntilTaskSemaphore.release();
	}

	public void dispatch(Task t) {
		this.taskToRun = t;
		waitUntilTaskSemaphore.release();
	}
}

static public class Pool {
	ArrayList<Worker> workers = new ArrayList<>();
	AtomicIntegerArray availableWorkers;
	Semaphore availableWorkersSemaphore;
	Semaphore sharedMutex = new Semaphore(1);
	boolean killPool = false;

	Pool(int size) {
		availableWorkersSemaphore = new Semaphore(size);
		availableWorkers = new AtomicIntegerArray(size);
		for (int i = 0; i < size; ++i) {
			workers.add(new Worker(this, i));
			availableWorkers.set(i, 1);       
		}
	}

	public boolean aquireSharedMutex() {
		return aquire(sharedMutex, 1);
	}

	public void releaseSharedMutex() {
		sharedMutex.release();
	}

	protected void done(int workerId, boolean status) {
		if (status == false) {
			throw new Error("WorkerError");
		}
		this.availableWorkers.compareAndSet(workerId, 0, 1);
		this.availableWorkersSemaphore.release();
	}

	public void run(Task t) {
		aquire(availableWorkersSemaphore, 1);
		for (int i = 0; i < this.workers.size(); ++i) {
			if (this.availableWorkers.compareAndSet(i, 1, 0)) {
				this.workers.get(i).dispatch(t);
				return;
			}
		}
		throw new Error("BrokenInvariantError");
	}

	public void killAll() {
		for (Worker worker : workers) {
			worker.kill();
		}
	}	

	public void awaitAll() {
		aquire(availableWorkersSemaphore, workers.size());
		this.availableWorkersSemaphore.release(workers.size());
	}

	public void close() {
		this.awaitAll();
		this.killAll();
		for (Worker worker : workers) {
			try {
				worker.join();
			} catch (InterruptedException e) {
				System.out.println("CaughtJoinError");
				continue;
			}
		}
	}
}
}
