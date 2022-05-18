package Task;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        task1();
    }

    static void task1() {
        int numbOfTasks = 5;
        int numbOfElems = 1000;
        AllStatCont alStCont = new AllStatCont(numbOfTasks);
        ExecutorService pool = Executors.newFixedThreadPool(numbOfTasks);
        Task t = new Task(alStCont, numbOfElems);
        for(int i=0; i<numbOfTasks; i++) {
            pool.execute(t);
        }

        pool.shutdown();

    }
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

        pool.execute(new Producer(drop, numbOfElems));
        for(int i=0; i<numbOfConsumers; i++) {
            pool.execute(new Consumer(drop));
        }

        ColStat cs = new ColStat(queue, pool, alStCont);
        Thread th = new Thread(cs);
        th.start();
        pool.shutdown();
    }
}



class ColStat implements Runnable {
    private final List<Integer> elementsInQueue = new ArrayList<>();
    private final Queue queue;
    private final ExecutorService pool;
    private final AllStatCont alStCont;

    public ColStat(Queue queue, ExecutorService pool, AllStatCont alStCont) {
        this.queue = queue;
        this.pool = pool;
        this.alStCont = alStCont;
    }

    @Override
    public void run() {
        int k = 0;
        while(true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            k++;
            int qSize = queue.getQueueSize();
            int rejCount = queue.getRejCount();
            int nOfEl = queue.getNumbOfElems();
            elementsInQueue.add(qSize);
            if(k % 3 == 0)
                alStCont.addToCont(rejCount, nOfEl, elementsInQueue, false);
            if(pool.isTerminated()) {
                alStCont.addToCont(rejCount, nOfEl, elementsInQueue, true);
                break;
            }
        }
    }
}

class Queue {
    List<Integer> queue = new ArrayList<>();
    private boolean endOfQueue = false;
    private int rejCount = 0;
    private final int maxSize = 18;
    private int numberOfElems = 0;

    public synchronized boolean getEndOfQueue() {
        return endOfQueue && queue.size() != 0 && queue.get(0) == Integer.MAX_VALUE;
    }

    public synchronized void setEndOfQueue() {
        endOfQueue = true;
    }

    public synchronized void add(int el) {
        if(queue.size() >= maxSize) {
            rejCount++;
        }
        else {
            queue.add(el);
        }
    }

    public synchronized int get() throws Exception {
        if(queue.size() != 0) {
            int el =  queue.get(0);
            if(el != Integer.MAX_VALUE)
                queue.remove(0);
            numberOfElems++;
            return el;
        }
        else throw new Exception("queue is empty");
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

    public synchronized int getNumbOfElems() {
        return numberOfElems;
    }
}

class Producer implements Runnable {
    private final Drop drop;
    private final int numbOfElems;

    public Producer(Drop drop, int numbOfElems) {
        this.drop = drop;
        this.numbOfElems = numbOfElems;
    }

    public void run() {
        int[] importantInfo = new int[numbOfElems];
        for(int i=0; i<numbOfElems; i++){
            importantInfo[i] = (int) (Math.random()*1000);
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

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
            while(!drop.getEndOfQueue()) {
                drop.take();
                try {
                    Thread.sleep((int) (Math.random()*710) + 50);
                } catch (InterruptedException e) {}

            }
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
    private List<AllStat> cont = new ArrayList<>();
    private List<AllStat> finalCont;
    private int nOfEl = 0;
    private int fNOfEl = 0;
    private final int numbOfWork;
    private int numbOfTrue = 0;

    public AllStatCont(int numbOfWork) {
        this.numbOfWork = numbOfWork;
        finalCont = new ArrayList<>(numbOfWork);
    }

    public synchronized void addToCont(int rejCount, int numbOfElems, List<Integer> elementsInQueue, boolean isEnd) {

        if(isEnd)
            numbOfTrue++;

        int sum = 0;
        for(int el: elementsInQueue) {
            sum += el;
        }
        nOfEl += numbOfElems;
        float evSize = (float) sum/elementsInQueue.size();
        float rejCountPer = (float) rejCount/numbOfElems;
        AllStat alst = new AllStat(evSize, rejCount, rejCountPer);
        cont.add(alst);
        if(numbOfWork == cont.size()) {
            fNOfEl = nOfEl;
            finalCont = new ArrayList<>(cont);
            calcAndShow(nOfEl, false);
            cont.clear();
            nOfEl = 0;
        }
        if(numbOfTrue == numbOfWork) {
            calcAndShow(fNOfEl, true);
            numbOfTrue++;
        }
    }

    public synchronized void calcAndShow(int numbOfElems, boolean isEnd) {

        float elemInQueue = 0;
        float rejCount = 0;

        for(AllStat c: finalCont) {
            elemInQueue += c.getElementsInQueue();
            rejCount += c.getRejCount();
        }
        elemInQueue /= finalCont.size();
        float rejCountPer = rejCount / numbOfElems;
        if(!isEnd) {
            System.out.println("Ev size = " + new DecimalFormat("#0.00").format(elemInQueue));
            System.out.println("Rej per= " + new DecimalFormat("#0.00000").format(rejCountPer)
                    + " or " + new DecimalFormat("#0.000").format(rejCountPer*100) + "%");
        }
        else {
            System.out.println();
            System.out.println("============================");
            System.out.println();
            System.out.println("Total ev size = " + new DecimalFormat("#0.00").format(elemInQueue));
            System.out.println("Rej per= " + new DecimalFormat("#0.00000").format(rejCountPer)
                    + " or " + new DecimalFormat("#0.000").format(rejCountPer*100) + "%");
        }
        System.out.println();
    }
}
