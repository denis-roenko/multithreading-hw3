package org.example.crawler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.RateLimiter;
import org.example.WikiClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class MultiThreadCrawler {

    private final WikiClient client = new WikiClient();
    private final RateLimiter rateLimiter;
    private final ConcurrentLinkedQueue<Node> searchQueue;
    private final ConcurrentHashMap<String, Boolean> visited;
    private final ExecutorService executor;
    private final AtomicReference<Node> result;

    public MultiThreadCrawler(int numThreads) {
        this(numThreads, new RateLimiter(1000, 1, TimeUnit.SECONDS));
    }

    public MultiThreadCrawler(int numThreads, RateLimiter rateLimiter) {
        this.searchQueue = new ConcurrentLinkedQueue<>();
        this.visited = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(numThreads);
        this.result = new AtomicReference<>(null);
        this.rateLimiter = rateLimiter;
    }

    public String find(String from, String target, long timeout, TimeUnit timeUnit) {
        return find(from, target, timeout, timeUnit, Integer.MAX_VALUE);
    }

    public String find(String from, String target, long timeout, TimeUnit timeUnit, int depth) {
        long deadline = System.nanoTime() + timeUnit.toNanos(timeout);

        searchQueue.add(new Node(from, null, 0));

        while (isNull(result.get())) {
            Node node = searchQueue.poll();

            if (nonNull(node)) {
                if (deadline < System.nanoTime()) {
                    System.out.println("Time limit exceeded!");
                    break;
                }
                if (node.getDepth() > depth) {
                    System.out.println("Depth limit exceeded!");
                    break;
                }

                while (!rateLimiter.allowRequest()) {
                    rateLimiter.await();
                }

                executor.submit(() -> searchNode(node, target));
            }
        }
        rateLimiter.shutdown();
        executor.shutdown();

        return getResult();
    }


    private void searchNode(Node node, String target) {
        if (nonNull(result.get())) return;

        System.out.println("Get page: " + node.getTitle());
        try {
            client.getByTitle(node.getTitle()).stream()
                    .filter(title -> !visited.containsKey(title.toLowerCase()))
                    .forEach(title -> {
                        visited.put(title.toLowerCase(), true);
                        Node subNode = new Node(title, node, node.getDepth() + 1);
                        if (target.equalsIgnoreCase(title.toLowerCase())) {
                            result.compareAndSet(null, subNode);
                        } else {
                            searchQueue.add(subNode);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getResult() {
        if (nonNull(result.get())) {
            List<String> resultList = new ArrayList<>();
            Node search = result.get();
            resultList.add(search.title);
            while (search.next != null) {
                search = search.next;
                resultList.add(search.title);
            }
            Collections.reverse(resultList);

            return join(" > ", resultList);
        }
        return "not found";
    }

    @Getter
    @AllArgsConstructor
    private static class Node {
        String title;
        Node next;
        int depth;
    }
}
