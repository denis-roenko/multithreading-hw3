package org.example.crawler;

import lombok.AllArgsConstructor;
import org.example.WikiClient;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.join;

public class SingleThreadCrawler {
    private final Queue<Node> searchQueue = new LinkedList<>();

    private final Set<String> visited = new HashSet<>();

    private final WikiClient client = new WikiClient();

    public String find(String from, String target, long timeout, TimeUnit timeUnit) throws Exception {
        long deadline = System.nanoTime() + timeUnit.toNanos(timeout);
        searchQueue.offer(new Node(from, null));
        Node result = null;
        while (result == null && !searchQueue.isEmpty()) {
            if (deadline < System.nanoTime()) {
                throw new TimeoutException();
            }
            Node node = searchQueue.poll();
            System.out.println("Get page: " + node.title);
            Set<String> links = client.getByTitle(node.title);
            if (links.isEmpty()) {
                //pageNotFound
                continue;
            }
            for (String link : links) {
                String currentLink = link.toLowerCase();
                if (visited.contains(currentLink)) {
                    continue;
                }
                visited.add(currentLink);
                Node subNode = new Node(link, node);
                if (target.equalsIgnoreCase(currentLink)) {
                    result = subNode;
                    continue;
                }
                searchQueue.offer(subNode);
            }
        }

        if (result != null) {
            List<String> resultList = new ArrayList<>();
            Node search = result;
            while (true) {
                resultList.add(search.title);
                if (search.next == null) {
                    break;
                }
                search = search.next;
            }
            Collections.reverse(resultList);

            return join(" > ", resultList);
        }

        return "not found";
    }

    @AllArgsConstructor
    private static class Node {
        String title;
        Node next;
    }
}
