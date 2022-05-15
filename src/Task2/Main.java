package Task2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        task1();
    }

    static void task1() throws ExecutionException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        Task1 t1 = new Task1();
        Future<AllStat> fi = pool.submit(t1);
        AllStat alst =  fi.get();
        show(alst.getRejCount(), 1000, alst.getElementsInQueue());
        pool.shutdown();


        /*int numbOfConsumers = 7;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        Queue queue = new Queue();
        Drop drop = new Drop(queue);

        pool.execute(new Producer(drop, numbOfConsumers));
        for(int i=0; i<numbOfConsumers; i++) {
            pool.execute(new Consumer(drop));
        }
        Callable<AllStat> cs = new ColStat(queue, pool);
        ExecutorService pool1 = Executors.newFixedThreadPool(8);
        Future<AllStat> fi = pool1.submit(cs);
        AllStat alst =  fi.get();

        pool.shutdown();
        pool1.shutdown();*/
    }

    /*static void task1() {
        int numbOfConsumers = 7;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        Queue queue = new Queue();
        Drop drop = new Drop(queue);

        pool.execute(new Producer(drop, numbOfConsumers));
        for(int i=0; i<numbOfConsumers; i++) {
            pool.execute(new Consumer(drop));
        }
        ColStat cs = new ColStat(queue);
        Thread csThr = new Thread(cs);
        csThr.setDaemon(true);
        csThr.start();
        pool.shutdown();
        showStat(pool, 1000, cs);
    }

    public static void showStat(ExecutorService pool, int numbOfElems, ColStat cs) {
        while (true) {
            if(pool.isTerminated()) {
                System.out.println("p rej = " + (float) cs.getRejCount()/numbOfElems + " or " + (float) cs.getRejCount()/numbOfElems*100 + "%" );
                List<Integer> evNumbOfElems = cs.getElInQueue();
                int sum = 0;
                for(int el: evNumbOfElems) {
                    sum += el;
                }
                System.out.println("ev size = " + sum/evNumbOfElems.size());
                break;
            }
        }
    }*/

    public static void show(int rejCount, int numbOfElems, List<Integer> elementsInQueue){

        System.out.println("p rej = " + (float) rejCount/numbOfElems + " or " + (float) rejCount/numbOfElems*100 + "%" );
        int sum = 0;
        for(int el: elementsInQueue) {
            sum += el;
        }
        System.out.println("ev size = " + sum/elementsInQueue.size());
    }
}

class Task1 implements Callable<AllStat> {
    @Override
    public AllStat call() throws ExecutionException, InterruptedException {
        System.out.println("Task1");
        int numbOfConsumers = 7;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        Queue queue = new Queue();
        Drop drop = new Drop(queue);

        pool.execute(new Producer(drop, numbOfConsumers));
        for(int i=0; i<numbOfConsumers; i++) {
            pool.execute(new Consumer(drop));
        }
        ColStat cs = new ColStat(queue, pool);
        ExecutorService pool1 = Executors.newFixedThreadPool(8);
        Future<AllStat> fi = pool1.submit(cs);
        AllStat alst =  fi.get();
        pool.shutdown();
        pool1.shutdown();
        return alst;
    }
}



class ColStat implements Callable<AllStat> {
    private final List<Integer> elementsInQueue = new ArrayList<>();
    private int rejCount = 0;
    private final Queue queue;
    private final ExecutorService pool;
    //int numbOfElems = 1000;

    public ColStat(Queue queue, ExecutorService pool) {
        this.queue = queue;
        this.pool = pool;
    }

    @Override
    public AllStat call() {
        System.out.println("Hello");
        while(true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int qSize = queue.getQueueSize();
            rejCount = queue.getRejCount();
            System.out.println("Queue size: " + qSize);
            System.out.println("RejCount: " + rejCount);
            elementsInQueue.add(qSize);
            if(pool.isTerminated()) {
                //show();
                break;
            }
        }

        return new AllStat(elementsInQueue, rejCount);
    }


}

class Queue {
    List<Integer> queue = new ArrayList<>();
    private boolean endOfQueue = false;
    private int rejCount = 0;
    private final int maxSize = 18;

    public boolean getEndOfQueue() {
        if(endOfQueue) {
            //System.out.println("Queue size: " + queue.size());
            //System.out.println("RejCount: " + rejCount);
        }
        return endOfQueue && (queue.size() == 0 || queue.get(queue.size() -1) == Integer.MAX_VALUE);
    }

    public synchronized void setEndOfQueue() {
        endOfQueue = true;
    }

    public synchronized void add(int el) {
        if(queue.size() >= maxSize) {
            rejCount++;
            //System.out.println("RejCount: " + rejCount);
        }
        else {
            queue.add(el);
            //System.out.println("Added: " + el);
        }
    }

    public synchronized int get() throws Exception {
        if(queue.size() != 0) {
            int el =  queue.get(queue.size() - 1);
            if(el != Integer.MAX_VALUE)
                queue.remove(queue.size() - 1);
            return el;
        }
        else throw new Exception("queue is empty");
    }

    public synchronized boolean full() {
        return queue.size() == maxSize;
    }

    public synchronized boolean empty() {
        return queue.size() == 0;
    }

    public synchronized  int getRejCount() {
        return rejCount;
    }

    public synchronized  int getQueueSize() {
        return queue.size();
    }
}

class Producer implements Runnable {
    private final Drop drop;
    private final int size = 1000;
    private final int numbOfCons;

    public Producer(Drop drop, int numbOfCons) {
        this.drop = drop;
        this.numbOfCons = numbOfCons;
    }

    public void run() {
        int[] importantInfo = new int[size];
        for(int i=0; i<size; i++){
            importantInfo[i] = (int) (Math.random()*1000);
            //System.out.print(importantInfo[i] + " ");
        }
        System.out.println();
        Random random = new Random();

        for (int i = 0;
             i < importantInfo.length;
             i++) {
            drop.put(importantInfo[i]);
            try {
                int sleepTime = (int) (Math.random()*100) + 5;
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {}
        }
        drop.setEndOfQueue();
        drop.put(Integer.MAX_VALUE);
    }
}

class Consumer implements Runnable {
    private final Drop drop;

    public Consumer(Drop drop) {
        this.drop = drop;
    }

    public void run() {

        try {
            int el;
            while(!drop.getEndOfQueue()) {
                el = drop.take();
                //System.out.println(Thread.currentThread().getName() + " " + el);
                //System.out.println(Thread.currentThread().getName() + " " + drop.getEndOfQueue());
                try {
                    Thread.sleep((int) (Math.random()*710) + 50);
                } catch (InterruptedException e) {}

            }
            //System.out.println("Closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Drop {
    private final Queue queue;

    public Drop(Queue queue) {
        this.queue = queue;
    }

    public boolean getEndOfQueue() {
        return queue.getEndOfQueue();
    }

    public void setEndOfQueue() {
        queue.setEndOfQueue();
    }

    public synchronized int take() throws Exception {
        if(queue.getEndOfQueue()) {
            return 0;
        }
        while (queue.empty()) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        int el = queue.get();
        notifyAll();
        return el;
    }

    public synchronized void put(int el) {
        queue.add(el);
        notifyAll();
    }
}

class AllStat {
    private final List<Integer> elementsInQueue;
    private final int rejCount;

    public AllStat(List<Integer> elementsInQueue, int rejCount) {
        this.elementsInQueue = elementsInQueue;
        this.rejCount = rejCount;
    }

    public List<Integer> getElementsInQueue() {
        return elementsInQueue;
    }

    public int getRejCount() {
        return rejCount;
    }
}