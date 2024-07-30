package org.example;

import org.example.crawler.MultiThreadCrawler;

import java.util.concurrent.TimeUnit;


public class Main {

    public static void main(String[] args) throws Exception {
//        SingleThreadCrawler crawler = new SingleThreadCrawler();

//        MultiThreadCrawler crawler = new MultiThreadCrawler(16);

        RateLimiter rateLimiter = new RateLimiter(1, 1, TimeUnit.SECONDS);
        MultiThreadCrawler crawler = new MultiThreadCrawler(3, rateLimiter);

        long startTime = System.nanoTime();
        String result = crawler.find("Java_(programming_language)", "Cat", 5, TimeUnit.MINUTES);
        long finishTime = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

        System.out.println("Took "+finishTime+" seconds, result is: " + result);
    }
}
