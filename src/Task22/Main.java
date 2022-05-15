package Task22;

import java.sql.SQLOutput;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        task1();
    }

    static void task1() {
        int numbOfTasks = 5;
        int numbOfElems = 1000;
        AllStatCont alStCont = new AllStatCont(numbOfTasks);
        Task t = new Task(alStCont, numbOfElems);
        ExecutorService pool = Executors.newFixedThreadPool(numbOfTasks);
        for(int i=0; i<numbOfTasks; i++) {
            pool.execute(t);
        }

        pool.shutdown();

        /*ExecutorService pool = Executors.newFixedThreadPool(8);
        Task1 t1 = new Task1();
        Future<AllStat> fi = pool.submit(t1);
        AllStat alst =  fi.get();
        show(alst.getRejCount(), 1000, alst.getElementsInQueue());
        pool.shutdown();*/



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

}

class Task implements Runnable {
    private final AllStatCont alStCont;
    private final int numbOfElems;
    public Task(AllStatCont alStCont, int numbOfElems) {
        this.alStCont = alStCont;
        this.numbOfElems = numbOfElems;
    }
    @Override
    public void run() {
        int numbOfConsumers = 7;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        Queue queue = new Queue();
        Drop drop = new Drop(queue);

        pool.execute(new Producer(drop, numbOfConsumers, numbOfElems));
        for(int i=0; i<numbOfConsumers; i++) {
            pool.execute(new Consumer(drop));
        }

        ColStat cs = new ColStat(queue, pool, alStCont, numbOfElems);
        Thread th = new Thread(cs);
        th.start();
        pool.shutdown();
    }
}



class ColStat implements Runnable {
    private final List<Integer> elementsInQueue = new ArrayList<>();
    private final Queue queue;
    private final ExecutorService pool;
    private final int numbOfElems;
    private final AllStatCont alStCont;

    public ColStat(Queue queue, ExecutorService pool, AllStatCont alStCont, int numbOfElems) {
        this.queue = queue;
        this.pool = pool;
        this.alStCont = alStCont;
        this.numbOfElems = numbOfElems;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int qSize = queue.getQueueSize();
            int rejCount = queue.getRejCount();
            //System.out.println("Queue size: " + qSize);
            //System.out.println("RejCount: " + rejCount);
            elementsInQueue.add(qSize);
            if(pool.isTerminated()) {
                //show(rejCount, numbOfElems, elementsInQueue);
                alStCont.addToCont(rejCount, numbOfElems, elementsInQueue);
                break;
            }
        }
    }

    /*public static void show(int rejCount, int numbOfElems, List<Integer> elementsInQueue){

        System.out.println("p rej = " + (float) rejCount/numbOfElems + " or " + (float) rejCount/numbOfElems*100 + "%" );
        int sum = 0;
        for(int el: elementsInQueue) {
            sum += el;
        }
        System.out.println("ev size = " + sum/elementsInQueue.size());
    }*/


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
    private final int numbOfCons;
    private final int numbOfElems;

    public Producer(Drop drop, int numbOfCons, int numbOfElems) {
        this.drop = drop;
        this.numbOfCons = numbOfCons;
        this.numbOfElems = numbOfElems;
    }

    public void run() {
        int[] importantInfo = new int[numbOfElems];
        for(int i=0; i<numbOfElems; i++){
            importantInfo[i] = (int) (Math.random()*1000);
            //System.out.print(importantInfo[i] + " ");
        }


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
    private final float avElemInQueue;
    private final int rejCount;
    private final float rejCountPer;

    public AllStat(float avElemInQueue, int rejCount, float rejCountPer) {
        this.avElemInQueue = avElemInQueue;
        this.rejCount = rejCount;
        this.rejCountPer = rejCountPer;
    }

    public float getElementsInQueue() {
        return avElemInQueue;
    }

    public int getRejCount() {
        return rejCount;
    }

    public float getRejCountPer() {return  rejCountPer;}
}

class AllStatCont {
    private final List<AllStat> cont = new ArrayList<>();
    private final int numbOfWork;

    public AllStatCont(int numbOfWork) {
        this.numbOfWork = numbOfWork;
    }

    public synchronized void addToCont(int rejCount, int numbOfElems, List<Integer> elementsInQueue) {
        int sum = 0;
        for(int el: elementsInQueue) {
            sum += el;
        }
        float evSize = (float) sum/elementsInQueue.size();
        float rejCountPer = (float) rejCount/numbOfElems;
        AllStat alst = new AllStat(evSize, rejCount, rejCountPer);
        cont.add(alst);
        if(numbOfWork == cont.size())
            calcAndShow(numbOfElems*numbOfWork);

    }

    public synchronized void calcAndShow(int numbOfElems) {

        for(AllStat c: cont) {
            System.out.println("ev size = " + new DecimalFormat("#0.00").format(c.getElementsInQueue()));
            System.out.println("rej count = " + c.getRejCount());
            System.out.println("rej count per = " + c.getRejCountPer() + " or " + c.getRejCountPer()*100 + "%");
            System.out.println();
        }

        float elemInQueue = 0;
        float rejCount = 0;

        for(AllStat c: cont) {
            elemInQueue += c.getElementsInQueue();
            rejCount += c.getRejCount();
        }
        elemInQueue /= cont.size();
        float rejCountPer = rejCount / numbOfElems;

        System.out.println("Total ev size = " + new DecimalFormat("#0.00").format(elemInQueue));
        System.out.println("Total rej = " + rejCountPer + " or " + rejCountPer*100 + "%");
    }
}
