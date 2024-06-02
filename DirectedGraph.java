package org.example;

import java.util.*;

public class DirectedGraph {
    private Map<String, Map<String, Integer>> adjacencyList;

    public DirectedGraph() {
        adjacencyList = new HashMap<>();
    }

    public void addVertex(String vertex) {
        if (!adjacencyList.containsKey(vertex)) {
            adjacencyList.put(vertex, new HashMap<>());
        }
    }

    public void addEdge(String from, String to) {
        if (!adjacencyList.containsKey(from)) {
            addVertex(from);
        }
        if (!adjacencyList.containsKey(to)) {
            addVertex(to);
        }
        int weight = 1;
        if (adjacencyList.get(from).containsKey(to)) {
            weight += adjacencyList.get(from).get(to);
        }
        adjacencyList.get(from).put(to, weight);
    }

    public void updateEdgeWeight(String from, String to, int newWeight) {
        if (adjacencyList.containsKey(from) && adjacencyList.get(from).containsKey(to)) {
            adjacencyList.get(from).put(to, newWeight);
        }
    }

    public Map<String, Integer> getNeighbors(String vertex) {
        return adjacencyList.getOrDefault(vertex, new HashMap<>());
    }

    public boolean hasEdge(String from, String to) {
        return adjacencyList.containsKey(from) && adjacencyList.get(from).containsKey(to);
    }

    public int getVertexCount() {
        return adjacencyList.size();
    }


    public Set<String> getVertices() {
        return adjacencyList.keySet();
    }

    public Map<String, Map<String, Integer>> getAdjacencyList() {
        return adjacencyList;
    }
}