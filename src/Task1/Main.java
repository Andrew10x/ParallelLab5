package Task1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        task1();
    }

    static void task1() {
        int numbOfConsumers = 7;
        ExecutorService pool = Executors.newFixedThreadPool(8);

        Drop drop = new Drop();

        pool.execute(new Producer(drop, numbOfConsumers));
        for(int i=0; i<numbOfConsumers; i++) {
            pool.execute(new Consumer(drop));
        }
        pool.shutdown();
    }
}

class colStat implements Runnable {
    List<Integer> elementsInQueue = new ArrayList<>();
    @Override
    public void run() {

    }
}

/*class Queue {
    List<Integer> queue = new ArrayList<>();
    private boolean endOfQueue = false;
    private int rejCount = 0;
    private final int maxSize = 20;

    public boolean getEndOfQueue() {
        if(endOfQueue) {
            System.out.println("Queue size: " + queue.size());
            System.out.println("RejCount: " + rejCount);
        }
        return endOfQueue && queue.size() == 0;
    }

    public void setEndOfQueue() {
        endOfQueue = true;
    }

    public synchronized void add(int el) {
        if(queue.size() >= maxSize) {
            rejCount++;
            System.out.println("RejCount: " + rejCount);
        }
        else {
            queue.add(el);
            System.out.println("Added: " + el);
        }
    }

    public synchronized int get() throws Exception {
        if(queue.size() != 0) {
            int el =  queue.get(queue.size() - 1);
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
}*/

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
            System.out.print(importantInfo[i] + " ");
        }
        System.out.println();
        Random random = new Random();

        for (int i = 0;
             i < importantInfo.length;
             i++) {
            drop.put(importantInfo[i]);
            try {
                int sleepTime = random.nextInt(100);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {}
        }

        drop.setEndOfQueue();
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
                if(el == Integer.MAX_VALUE)
                    break;
                System.out.println(Thread.currentThread().getName() + " " + el);
                try {
                    Thread.sleep((int) (Math.random()*50));
                } catch (InterruptedException e) {}

            }
            System.out.println("Closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Drop {
    BlockingQueue<Integer> queue;
    private static boolean endOfQueue = false;
    private int rejCount = 0;
    private final int maxSize = 20;

    public Drop() {
        queue = new ArrayBlockingQueue<Integer>(20);
    }
    public int take() throws Exception {
        //Thread.sleep((int) (Math.random()*700));
        return queue.take();
    }

    public void put(int el) {

        if(!queue.offer(el)) {
            rejCount++;
            System.out.println("RejCount: " + el);
        }
    }

    public boolean getEndOfQueue() {
        if(endOfQueue) {
            System.out.println("Queue size: " + queue.size());
            System.out.println("RejCount: " + rejCount);
        }
        return endOfQueue && queue.size() == 0;
    }

    public void setEndOfQueue() {
        endOfQueue = true;
    }/*

    public synchronized boolean full() {
        return queue.size() == maxSize;
    }

    public synchronized boolean empty() {
        return queue.size() == 0;
    }

    public synchronized  int getRejCount() {
        return rejCount;
    }*/
}