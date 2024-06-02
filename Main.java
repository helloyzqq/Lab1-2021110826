package org.example;

import guru.nidi.graphviz.attribute.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import static guru.nidi.graphviz.model.Factory.*;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Map;
import java.util.function.Function;


public class Main {
    // 主函数
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java TextToDirectedGraph <filepath>");
            return;
        }
        System.out.println("hello, world");
        String filePath = args[0];
        try {
            // 根据命令行传入的文本文件创建有向图
            DirectedGraph graph = buildGraph(filePath);
            Scanner scanner = new Scanner(System.in);

            // 提示用户输入一个选项
            System.out.println("请输入一个数字（1-5）来选择执行的函数:");
            System.out.println("1: 展示有向图");
            System.out.println("2: 查询桥接词");
            System.out.println("3: 计算单词间最短路径");
            System.out.println("4: 根据bridge word生成新文本");
            System.out.println("5: 随机游走");
            int userInput = scanner.nextInt();
            scanner.nextLine();
            String word1, word2;
            // 根据用户输入执行不同的函数
            switch (userInput) {
                case 1:
                    // 展示有向图
                    System.out.println("展示有向图");
                    showDirectedGraph(graph);
                    break;
                case 2:
                    // 查询桥接词
                    System.out.println("查询桥接词");
                    System.out.println("请输入第一个单词");
                    word1 = scanner.nextLine();
                    System.out.println("请输入第二个单词");
                    word2 = scanner.nextLine();
                    queryBridgeWords(graph, word1, word2);
                    break;
                case 3:
                    // 计算单词间最短路径
                    System.out.println("计算单词间最短路径");
                    System.out.println("请输入第一个单词");
                    word1 = scanner.nextLine();
                    System.out.println("请输入第二个单词");
                    word2 = scanner.nextLine();
                    calcShortestPath(graph, word1, word2);
                    break;
                case 4:
                    // 根据bridge word生成新文本
                    System.out.println("根据bridge word生成新文本");
                    System.out.println("请输入原文本，使用空格分离");
                    String inputText = scanner.nextLine();
                    generateNewText(graph, inputText);
                    break;
                case 5:
                    // 随机游走
                    System.out.println("随机游走");
                    randomWalk(graph);
                    break;
                default:
                    System.out.println("输入无效，请输入1-5之间的数字。");
                    break;
            }

            scanner.close();
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }

    }

    // 根据从命令行中输入的文本文件创建有向图并返回。
    private static DirectedGraph buildGraph(String filePath) throws IOException {
        DirectedGraph graph = new DirectedGraph();
        Pattern pattern = Pattern.compile("[^a-zA-Z\\s]");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String previousWord = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("\\p{P}", " ");
                line = pattern.matcher(line).replaceAll("");
                String[] words = line.toLowerCase().trim().split("\\s+");

                for (String word : words) {
                    if (!word.isEmpty()) {
                        if (previousWord != null) {
                            if (!graph.hasEdge(previousWord, word)) {
                                graph.addEdge(previousWord, word);
                            } else {
                                int currentWeight = graph.getNeighbors(previousWord).get(word);
                                graph.updateEdgeWeight(previousWord, word, currentWeight + 1);
                            }
                        }
                        previousWord = word;
                    }
                }
            }
        }
        return graph;
    }

    // 展示有向图
    private static void showDirectedGraph(DirectedGraph graph) {
        MutableGraph g = mutGraph("example1").setDirected(true);

        // 遍历所有的顶点以及邻接的顶点和权重
        for (String source : graph.getVertices()) {
            Map<String, Integer> targets = graph.getNeighbors(source);
            for (Map.Entry<String, Integer> entry : targets.entrySet()) {
                String target = entry.getKey();
                Integer weight = entry.getValue();
                g.add(mutNode(source).addLink(to(mutNode(target)).with(Label.of(String.valueOf(weight)))));
            }
        }

        // 使用Graphviz生成图像并保存为文件
        try {
            Graphviz.fromGraph(g).width(800).render(Format.PNG).toFile(new File("graph.png"));
            System.out.println("Graph visualization saved as graph.png");
        } catch (IOException e) {
            System.out.println("Unable to save graph to a file: " + e.getMessage());
        }
    }

    // 找出两个单词间的桥接词
    public static List<String> queryBridgeWords(DirectedGraph graph, String word1, String word2) {
        if (!graph.getAdjacencyList().containsKey(word1) || !graph.getAdjacencyList().containsKey(word2)) {
            System.out.println("Either " + word1 + " or " + word2 + " does not exist in the graph!");
            return null;
        }

        Set<String> possibleBridges = new HashSet<>();
        Map<String, Integer> neighborsOfWord1 = graph.getNeighbors(word1);

        // 检查word1的每一个相邻顶点
        for (String middleWord : neighborsOfWord1.keySet()) {
            // 如果middleWord和Word2之间有边，则添加到桥接词
            if (graph.hasEdge(middleWord, word2)) {
                possibleBridges.add(middleWord);
            }
        }

        if (possibleBridges.isEmpty()) {
            System.out.println("No bridge words from " + word1 + " to " + word2 + "!");
            return null;
        } else {
            System.out.println("The bridge words from " + word1 + " to " + word2 + " are: " + String.join(", ", possibleBridges) + ".");
            return new ArrayList<>(possibleBridges);
        }
    }

    // 计算两单词间最短路径
    public static void calcShortestPath(DirectedGraph graph, String source, String target) {
        String dotFilename = source + "_" + target + "_shortestPath.dot";
        if ("NULL".equals(target)) {
            // 计算从源节点到所有其他节点的最短路径
            for (String vertex : graph.getVertices()) {
                if (!vertex.equals(source)) {
                    List<String> path = dijkstraShortestPath(graph, source, vertex);
                    if (path == null) {
                        System.out.println("No accessible path exists between " + source + " and " + vertex);
                    } else {
                        System.out.println("The shortest path from " + source + " to " + vertex + " is: " + path);
                    }
                }
            }
        } else {
            List<String> path = dijkstraShortestPath(graph, source, target);
            if (path == null) {
                System.out.println("No accessible path exists between " + source + " and " + target);
            } else {
                System.out.println("The shortest path from " + source + " to " + target + " is: " + path);
                visualizeAndHighlightPath(graph, path, dotFilename);
            }
        }

    }

    private static List<String> dijkstraShortestPath(DirectedGraph graph, String source, String target) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<VertexDistance> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(VertexDistance::getDistance));
        Set<String> visited = new HashSet<>();

        for (String vertex : graph.getVertices()) {
            distances.put(vertex, Integer.MAX_VALUE);
            predecessors.put(vertex, null);
            priorityQueue.add(new VertexDistance(vertex, Integer.MAX_VALUE));
        }

        distances.replace(source, 0);
        priorityQueue.add(new VertexDistance(source, 0)); // 正确的更新源节点状态

        while (!priorityQueue.isEmpty()) {
            VertexDistance current = priorityQueue.poll();
            if (current.distance == Integer.MAX_VALUE) {
                break; // 如果当前节点的距离是 Integer.MAX_VALUE，表示后面的节点都不可达
            }
            if (!visited.add(current.vertex)) {
                continue;
            }

            for (Map.Entry<String, Integer> neighbor : graph.getNeighbors(current.vertex).entrySet()) {
                int newDist = distances.get(current.vertex) + neighbor.getValue();
                if (newDist < distances.get(neighbor.getKey())) {
                    distances.replace(neighbor.getKey(), newDist);
                    predecessors.put(neighbor.getKey(), current.vertex);
                    priorityQueue.add(new VertexDistance(neighbor.getKey(), newDist));
                }
            }
        }

        if (distances.get(target) == Integer.MAX_VALUE) {
            return null; // 如果目标节点的距离未曾被更新表示不可达
        }
        return buildPath(predecessors, source, target);
    }

    private static List<String> buildPath(Map<String, String> predecessors, String source, String target) {
        LinkedList<String> path = new LinkedList<>();
        String step = target;
        if (predecessors.get(step) == null) {
            return null;
        }
        path.add(step);
        while (!step.equals(source)) {
            step = predecessors.get(step);
            if (step == null) {
                return null; // 如果中间断了，说明无有效路径，防止无限循环
            }
            path.add(step);
        }
        Collections.reverse(path);
        return path;
    }

    private static class VertexDistance {
        String vertex;
        int distance;

        VertexDistance(String vertex, int distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        String getVertex() {
            return vertex;
        }

        int getDistance() {
            return distance;
        }
    }

    // 最短路径标红并存为DOT文件
    public static void visualizeAndHighlightPath(DirectedGraph graph, List<String> path, String dotFilename) {
        Function<String, String> vertexIdProvider = v -> v;
        Function<String, String> vertexLabelProvider = v -> v;

        try (Writer writer = new FileWriter(dotFilename)) {
            writer.write("digraph G {\n");
            for (String vertex : graph.getVertices()) {
                writer.write("  " + vertexIdProvider.apply(vertex) + " [label=\"" + vertexLabelProvider.apply(vertex) + "\"];\n");
            }
            for (String from : graph.getVertices()) {
                Map<String, Integer> edges = graph.getNeighbors(from);
                for (Map.Entry<String, Integer> edge : edges.entrySet()) {
                    String to = edge.getKey();
                    int weight = edge.getValue();
                    String color = "black";
                    if (isEdgeInPath(from, to, path)) {
                        color = "red";
                    }
                    writer.write("  " + from + " -> " + to + " [label=\"" + weight + "\", color=\"" + color + "\"];\n");
                }
            }
            writer.write("}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        renderGraph(dotFilename);
    }

    private static boolean isEdgeInPath(String from, String to, List<String> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            if (path.get(i).equals(from) && path.get(i + 1).equals(to)) {
                return true;
            }
        }
        return false;
    }

    // 渲染dot文件
    private static void renderGraph(String dotFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFile, "-o", dotFile.replace(".dot", ".png"));
            pb.redirectErrorStream(true);
            Process process = pb.start();
//            process.waitFor();
            System.out.println("Graph visualized with the path highlighted.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to render the graph.");
        }
    }

    public static void generateNewText(DirectedGraph graph, String inputText) {
        Random rand = new Random();
        String[] words = inputText.split(" ");
        List<String> finalText = new ArrayList<>();

        for (int i = 0; i < words.length - 1; i++) {
            finalText.add(words[i]);
            List<String> inserts = queryBridgeWords(graph, words[i], words[i + 1]);
            if (inserts != null) {
                finalText.add(inserts.get(rand.nextInt(inserts.size())));
            }
        }

        // 添加最后一个单词
        if (words.length > 0) {
            finalText.add(words[words.length - 1]);
        }

        // 构建最终文本输出
        String result = String.join(" ", finalText);
        System.out.println(result);
    }


    // 执行随机游走的函数
    public static void randomWalk(DirectedGraph graph) {
        Set<String> visitedEdges = new HashSet<>();
        List<String> visitedNodes = new ArrayList<>();
        Random rand = new Random();
        String current = getRandomVertex(graph, rand);

        // 随机选择初始节点
        visitedNodes.add(current);

        System.out.println("初始节点选择：" + current + "。开始进行随机游走.");

        Scanner scanner = new Scanner(System.in);
        boolean continueWalk = true;

        while (continueWalk) {
            Map<String, Integer> edges = graph.getNeighbors(current);

            if (edges.isEmpty()) break; // 如果没有出边，终止游走

            List<String> possibleNodes = new ArrayList<>(edges.keySet());
            String next = possibleNodes.get(rand.nextInt(possibleNodes.size()));
            String edgeIdentifier = current + "->" + next;

            if (visitedEdges.contains(edgeIdentifier)) {
                current = next;
                visitedNodes.add(current);
                System.out.println("当前节点" + current + "。遇到重复边，终止游走.");
                break; // 如果边已访问，终止游走
            }
            visitedEdges.add(edgeIdentifier);

            current = next;
            visitedNodes.add(current);

            System.out.println("当前节点: " + current + "。按 enter 继续或输入 'stop' 停止游走.");
            String input = scanner.nextLine();
            continueWalk = !input.trim().equalsIgnoreCase("stop");
        }

        // 输出遍历的节点
        System.out.println("访问的节点: " + visitedNodes);
        writeToFile(visitedNodes);
        scanner.close();
    }

    // 从图中随机选取一个顶点
    private static String getRandomVertex(DirectedGraph graph, Random rand) {
        Object[] vertices = graph.getVertices().toArray();
        return (String) vertices[rand.nextInt(vertices.length)];
    }

    // 将节点信息写入文件
    private static void writeToFile(List<String> nodes) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("randomWalk_result.txt"))) {
            for (String node : nodes) {
                writer.write(node + System.lineSeparator());
            }
            System.out.println("遍历的节点已保存至文件: randomWalk_result.txt");
        } catch (IOException e) {
            System.err.println("写文件时发生错误: " + e.getMessage());
        }
    }


}
