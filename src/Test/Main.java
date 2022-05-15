package Test;

import java.util.concurrent.*;

public class Main {


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Callable<String> callable = new MyCallable();
        Future<String> fut = executor.submit(callable);
        System.out.println(fut.get());
        executor.shutdown();
    }
}

class MyCallable implements Callable<String> {

    @Override
    public String call() throws Exception {
        Thread.sleep(1000);
        return Thread.currentThread().getName();
    }
}
