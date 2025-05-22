import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


@SuppressWarnings({"checkstyle:Indentation", "checkstyle:MissingJavadocType", "checkstyle:LineLength"})
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    static InputStream inputStream = System.in;
    @SuppressWarnings("checkstyle:Indentation")
    static Map<String, Map<String, Integer>> adjList = new HashMap<>();
    @SuppressWarnings("checkstyle:Indentation")
    static Set<String> nodes = new HashSet<>();
    @SuppressWarnings("checkstyle:Indentation")
    static double damping = 0.85;
    @SuppressWarnings("checkstyle:Indentation")
    static Scanner scanner = new Scanner(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

    @SuppressWarnings({"checkstyle:Indentation", "checkstyle:NeedBraces", "checkstyle:LineLength", "checkstyle:LocalVariableName", "checkstyle:MultipleVariableDeclarations", "checkstyle:OperatorWrap", "checkstyle:MissingJavadocMethod"})
    public static void main(String[] args) {
        System.out.print("Enter input text file: ");
        String filename = scanner.nextLine();
        List<String> words = tokenizeFile(filename);
        System.out.println("Tokenized words count: " + words.size());
        buildGraph(words);
        printGraph(null);

        while (true) {
            System.out.println("\nMenu:\n" +
                    "1. Query bridge words\n" +
                    "2. Generate new text\n" +
                    "3. Shortest path\n" +
                    "4. Compute PageRank\n" +
                    "5. Random walk\n" +
                    "6. Exit");
            System.out.print("Select option: ");
            int opt;
            try {
                opt = Integer.parseInt(scanner.nextLine());
            } catch (Exception e) {
                break;
            }

            switch (opt) {
                case 1:
                    System.out.print("Enter A and B: ");
                    String[] parts1 = scanner.nextLine().split("\\s+");
                    if (parts1.length != 2) {
                        System.out.println("Invalid number of words. You must enter exactly two words.");
                        break;
                    }
                    for (int i = 0; i < parts1.length; i++) {
                        parts1[i] = parts1[i].toLowerCase();
                    }
                    String A1 = parts1[0], B1 = parts1[1];
                    List<String> bridges = queryBridge(A1, B1);
                    boolean hasA = nodes.contains(A1), hasB = nodes.contains(B1);
                    if (!hasA && !hasB) System.out.println("No " + A1 + " and " + B1 + " in the graph!");
                    else if (!hasA) System.out.println("No " + A1 + " in the graph!");
                    else if (!hasB) System.out.println("No " + B1 + " in the graph!");
                    else if (bridges.isEmpty()) System.out.println("No bridge words from " + A1 + " to " + B1 + "!");
                    else if (bridges.size() == 1)
                        System.out.println("The bridge word from " + A1 + " to " + B1 + " is: " + bridges.get(0) + ".");
                    else {
                        System.out.print("The bridge words from " + A1 + " to " + B1 + " are: ");
                        for (int i = 0; i < bridges.size(); i++) {
                            System.out.print(bridges.get(i));
                            if (i + 1 < bridges.size()) System.out.print(i + 2 == bridges.size() ? " and " : ", ");
                        }
                        System.out.println(".");
                    }
                    break;
                case 2:
                    System.out.print("Enter a new text file: ");
                    String fn = scanner.nextLine();
                    List<String> w2 = tokenizeFile(fn);
                    System.out.println("Generated text:\n" + generateNewText(w2));
                    break;
                case 3:
                    System.out.print("Enter start and end: ");
                    String[] parts2 = scanner.nextLine().split("\\s+");
                    if (parts2.length < 2) break;
                    String A2 = parts2[0], B2 = parts2[1];
                    Pair<List<String>, Integer> result = shortestPathWithWeight(A2, B2);
                    if (result.first.isEmpty()) {
                        System.out.println("No path from " + A2 + " to " + B2 + "!");
                    } else {
                        Set<String> highlightEdges = new HashSet<>();
                        for (int i = 0; i < result.first.size() - 1; i++) {
                            highlightEdges.add(result.first.get(i) + "->" + result.first.get(i + 1));
                        }
                        printGraph(highlightEdges);
                        System.out.println("\nShortest Path Highlighted:");
                        for (int i = 0; i < result.first.size(); i++) {
                            System.out.print(result.first.get(i));
                            if (i + 1 < result.first.size()) System.out.print(" -> ");
                        }
                        System.out.println("\nTotal path weight: " + result.second);
                    }
                    break;
                case 4:
                    Map<String, Double> pr = computePageRank();
                    System.out.println("PageRank values:");
                    for (Map.Entry<String, Double> e : pr.entrySet()) {
                        System.out.println(e.getKey() + ": " + e.getValue());
                    }
                    break;
                case 5:
                    System.out.println("Performing random walk...");
                    List<String> walk = randomWalk();
                    System.out.print("Walk path: ");
                    for (int i = 0; i < walk.size(); i++) {
                        System.out.print(walk.get(i));
                        if (i + 1 < walk.size()) System.out.print(" -> ");
                    }
                    try (PrintWriter writer = new PrintWriter(
                            new OutputStreamWriter(Files.newOutputStream(Paths.get("output.txt")), StandardCharsets.UTF_8)
                    )) {
                        for (String w : walk) writer.println(w);
                        System.out.println("\nSaved to random_walk.txt");
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error during file write", e);
                    }
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    @SuppressWarnings("checkstyle:Indentation")
    static List<String> tokenizeFile(String filename) {
        Path baseDir = Paths.get("").toAbsolutePath().resolve("data").normalize();
        Path resolvedPath = baseDir.resolve(filename).normalize();
        if (!resolvedPath.startsWith(baseDir)) {
            throw new SecurityException("非法路径: " + filename);
        }
        List<String> words = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(resolvedPath, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("[^a-zA-Z]", " ").toLowerCase();
                sb.append(line).append(" ");
            }
            String[] splitWords = sb.toString().trim().split("\\s+");
            Collections.addAll(words, splitWords);
        } catch (IOException e) {
            System.err.println("文件读取失败: " + resolvedPath);
            throw new RuntimeException(e);
        }
        return words;
    }

    @SuppressWarnings({"checkstyle:Indentation", "checkstyle:OneStatementPerLine", "checkstyle:MultipleVariableDeclarations"})
    static void buildGraph(List<String> words) {
        for (int i = 0; i + 1 < words.size(); i++) {
            String a = words.get(i), b = words.get(i + 1);
            adjList.putIfAbsent(a, new HashMap<>());
            adjList.get(a).put(b, adjList.get(a).getOrDefault(b, 0) + 1);
            nodes.add(a); nodes.add(b);
        }
    }

    @SuppressWarnings({"checkstyle:Indentation", "checkstyle:LocalVariableName", "checkstyle:NeedBraces", "checkstyle:ParameterName"})
    static List<String> queryBridge(String A, String B) {
        List<String> bridges = new ArrayList<>();
        if (!nodes.contains(A) || !nodes.contains(B)) return bridges;
        Map<String, Integer> mid = adjList.getOrDefault(A, new HashMap<>());
        for (String C : mid.keySet()) {
            if (adjList.containsKey(C) && adjList.get(C).containsKey(B)) {
                bridges.add(C);
            }
        }
        return bridges;
    }

    @SuppressWarnings({"checkstyle:Indentation", "checkstyle:NeedBraces", "checkstyle:LocalVariableName", "checkstyle:MultipleVariableDeclarations"})
    static String generateNewText(List<String> words) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i + 1 < words.size(); i++) {
            String X = words.get(i), Y = words.get(i + 1);
            out.add(X);
            List<String> bridges = queryBridge(X, Y);
            if (!bridges.isEmpty()) {
                out.add(bridges.get(SECURE_RANDOM.nextInt(bridges.size())));
            }
        }
        if (!words.isEmpty()) out.add(words.get(words.size() - 1));
        return String.join(" ", out);
    }

    @SuppressWarnings({"checkstyle:Indentation", "checkstyle:NeedBraces", "checkstyle:LineLength", "checkstyle:AbbreviationAsWordInName", "checkstyle:VariableDeclarationUsageDistance"})
    static Pair<List<String>, Integer> shortestPathWithWeight(String start, String end) {
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> pre = new HashMap<>();
        final int INF = Integer.MAX_VALUE;
        for (String node : nodes) dist.put(node, INF);
        if (!nodes.contains(start) || !nodes.contains(end)) return new Pair<>(new ArrayList<>(), -1);

        dist.put(start, 0);
        PriorityQueue<Pair<Integer, String>> pq = new PriorityQueue<>(Comparator.comparingInt(p -> p.first));
        pq.add(new Pair<>(0, start));

        while (!pq.isEmpty()) {
            Pair<Integer, String> p = pq.poll();
            int d = p.first;
            String u = p.second;
            if (d > dist.get(u)) continue;
            if (u.equals(end)) break;

            Map<String, Integer> neighbors = adjList.getOrDefault(u, new HashMap<>());
            for (Map.Entry<String, Integer> e : neighbors.entrySet()) {
                String v = e.getKey();
                int w = e.getValue();
                if (dist.get(u) + w < dist.get(v)) {
                    dist.put(v, dist.get(u) + w);
                    pre.put(v, u);
                    pq.add(new Pair<>(dist.get(v), v));
                }
            }
        }

        if (dist.get(end) == INF) return new Pair<>(new ArrayList<>(), -1);
        List<String> path = new ArrayList<>();
        for (String at = end; !at.equals(start); at = pre.get(at)) {
            path.add(at);
        }
        path.add(start);
        Collections.reverse(path);
        return new Pair<>(path, dist.get(end));
    }

    @SuppressWarnings({"checkstyle:Indentation", "checkstyle:NeedBraces", "checkstyle:LocalVariableName"})
    static Map<String, Double> computePageRank() {
        int N = nodes.size();
        Map<String, Double> pr = new HashMap<>();
        Map<String, Double> newPr = new HashMap<>();
        for (String n : nodes) pr.put(n, 1.0 / N);

        for (int it = 0; it < 100; it++) {
            double diff = 0;
            for (String u : nodes) newPr.put(u, (1.0 - damping) / N);

            for (String u : nodes) {
                double share = pr.get(u);
                Map<String, Integer> out = adjList.getOrDefault(u, new HashMap<>());
                if (out.isEmpty()) {
                    double part = share / N;
                    for (String v : nodes) newPr.put(v, newPr.get(v) + damping * part);
                } else {
                    double part = share / out.size();
                    for (String v : out.keySet()) newPr.put(v, newPr.get(v) + damping * part);
                }
            }

            for (String u : nodes) {
                diff += Math.abs(newPr.get(u) - pr.get(u));
                pr.put(u, newPr.get(u));
            }
            if (diff < 1e-6) break;
        }
        return pr;
    }

    @SuppressWarnings({"checkstyle:Indentation", "checkstyle:NeedBraces"})
    static List<String> randomWalk() {
        List<String> path = new ArrayList<>();
        Set<String> visitedEdges = new HashSet<>();
        if (nodes.isEmpty()) return path;
        List<String> nodeList = new ArrayList<>(nodes);
        String cur = nodeList.get(SECURE_RANDOM.nextInt(nodeList.size()));
        path.add(cur);

        while (true) {
            Map<String, Integer> out = adjList.getOrDefault(cur, new HashMap<>());
            if (out.isEmpty()) break;
            List<String> nextList = new ArrayList<>(out.keySet());
            String nxt = nextList.get(SECURE_RANDOM.nextInt(nextList.size()));
            String edge = cur + "->" + nxt;
            if (!visitedEdges.add(edge)) break;
            path.add(nxt);
            cur = nxt;
        }

        return path;
    }

    @SuppressWarnings({"checkstyle:Indentation", "checkstyle:NeedBraces", "checkstyle:LineLength"})
    static void printGraph(Set<String> highlightEdges) {
        System.out.println("\nDirected Graph Structure:");
        for (Map.Entry<String, Map<String, Integer>> src : adjList.entrySet()) {
            for (Map.Entry<String, Integer> tgt : src.getValue().entrySet()) {
                String edge = src.getKey() + "->" + tgt.getKey();
                System.out.print(src.getKey() + " -> " + tgt.getKey() + " (weight: " + tgt.getValue() + ")");
                if (highlightEdges != null && highlightEdges.contains(edge)) System.out.print(" ** [PATH]");
                System.out.println();
            }
        }
    }

    @SuppressWarnings({"checkstyle:RightCurly", "checkstyle:Indentation", "checkstyle:LineLength"})
    static class Pair<F, S> {
        @SuppressWarnings("checkstyle:Indentation")
        public F first;
        @SuppressWarnings("checkstyle:Indentation")
        public S second;
        @SuppressWarnings({"checkstyle:OneStatementPerLine", "checkstyle:LeftCurly", "checkstyle:Indentation", "checkstyle:EmptyLineSeparator"})
        public Pair(F f, S s) { first = f; second = s; }
    }
}
