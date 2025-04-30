#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>
#include <queue>
#include <limits>
#include <algorithm>
#include <cctype>
#include <random>
#include <functional>  // For std::greater

using namespace std;

// 邻接表：源 -> (目标 -> 权重)
using AdjList = unordered_map<string, unordered_map<string, int>>;

// 全局变量
AdjList adjList;
unordered_set<string> nodes;

double damping = 0.85;

// 文本预处理：读取文件，将非字母转为空格，转小写，分词
vector<string> tokenizeFile(const string& filename) {
    ifstream fin(filename);
    if (!fin) {
        cerr << "Cannot open file: " << filename << endl;
        exit(EXIT_FAILURE);
    }
    string line, text;
    while (getline(fin, line)) {
        for (char& c : line) {
            c = (isalpha(static_cast<unsigned char>(c)) ? tolower(static_cast<unsigned char>(c)) : ' ');
        }
        text += line;
        text += ' ';
    }

    vector<string> words;
    istringstream iss(text);
    string w;
    while (iss >> w) {
        words.push_back(w);
    }
    return words;
}

// 构建图
void buildGraph(const vector<string>& words) {
    for (size_t i = 0; i + 1 < words.size(); ++i) {
        const string& a = words[i];
        const string& b = words[i + 1];
        adjList[a][b]++;
        nodes.insert(a);
        nodes.insert(b);
    }
}

void printGraph(const unordered_set<string>& highlightEdges = {}) {
    cout << "\nDirected Graph Structure:" << endl;
    for (const auto& source_entry : adjList) {
        const string& source = source_entry.first;
        const auto& targets = source_entry.second;
        for (const auto& target_entry : targets) {
            const string& target = target_entry.first;
            int weight = target_entry.second;
            string edgeKey = source + "->" + target;
            cout << source << " -> " << target << " (weight: " << weight << ")";
            if (highlightEdges.count(edgeKey)) {
                cout << "  ** [PATH]";  // 突出显示标记
            }
            cout << endl;
        }
    }
}
// 查询桥接词
vector<string> queryBridge(const string& A, const string& B) {
    vector<string> bridges;
    if (!nodes.count(A) || !nodes.count(B)) return bridges;
    auto it = adjList.find(A);
    if (it == adjList.end()) return bridges;
    for (const auto& p : it->second) {
        const string& C = p.first;
        auto it2 = adjList.find(C);
        if (it2 != adjList.end() && it2->second.count(B)) {
            bridges.push_back(C);
        }
    }
    return bridges;
}

// 生成新文本
string generateNewText(const vector<string>& words) {
    vector<string> out;
    random_device rd;
    mt19937 gen(rd());
    for (size_t i = 0; i + 1 < words.size(); ++i) {
        const string& X = words[i];
        const string& Y = words[i + 1];
        out.push_back(X);
        auto bridges = queryBridge(X, Y);
        if (!bridges.empty()) {
            int maxIndex = static_cast<int>(bridges.size()) - 1;
            uniform_int_distribution<int> dis(0, maxIndex);
            out.push_back(bridges[dis(gen)]);
        }
    }
    if (!words.empty()) out.push_back(words.back());

    ostringstream oss;
    for (size_t i = 0; i < out.size(); ++i) {
        if (i) oss << ' ';
        oss << out[i];
    }
    return oss.str();
}

// 最短路径（Dijkstra）
pair<vector<string>, int> shortestPathWithWeight(const string& start, const string& end) {
    const int INF = numeric_limits<int>::max();
    unordered_map<string, int> dist;
    unordered_map<string, string> pre;
    for (const auto& n : nodes) dist[n] = INF;

    if (!nodes.count(start) || !nodes.count(end)) return { {}, -1 };

    dist[start] = 0;
    using P = pair<int, string>;
    priority_queue<P, vector<P>, greater<P>> pq;
    pq.push({ 0, start });

    while (!pq.empty()) {
        auto elem = pq.top(); pq.pop();
        int d = elem.first;
        string u = elem.second;

        if (d > dist[u]) continue;
        if (u == end) break;

        for (const auto& e : adjList[u]) {
            const string& v = e.first;
            int w = e.second;
            if (dist[u] + w < dist[v]) {
                dist[v] = dist[u] + w;
                pre[v] = u;
                pq.push({ dist[v], v });
            }
        }
    }

    if (dist[end] == INF) return { {}, -1 };

    vector<string> path;
    int totalWeight = dist[end];
    for (string cur = end; cur != start; cur = pre[cur]) {
        path.push_back(cur);
    }
    path.push_back(start);
    reverse(path.begin(), path.end());

    return { path, totalWeight };
}


// PageRank计算
unordered_map<string, double> computePageRank(int maxIter = 100, double eps = 1e-6) {
    size_t N = nodes.size();
    unordered_map<string, double> pr, newPr;
    for (const auto& n : nodes) pr[n] = 1.0 / N;
    for (int it = 0; it < maxIter; ++it) {
        double diff = 0;
        for (const auto& u : nodes) {
            newPr[u] = (1.0 - damping) / N;
        }
        for (const auto& u : nodes) {
            double share = pr[u];
            auto it = adjList.find(u);
            if (it == adjList.end() || it->second.empty()) {
                double part = share / N;
                for (const auto& v : nodes) newPr[v] += damping * part;
            }
            else {
                int outdeg = static_cast<int>(it->second.size());
                double part = share / outdeg;
                for (const auto& e : it->second) newPr[e.first] += damping * part;
            }
        }
        for (const auto& u : nodes) {
            diff += fabs(newPr[u] - pr[u]);
            pr[u] = newPr[u];
        }
        if (diff < eps) break;
    }
    return pr;
}

// 随机游走
vector<string> randomWalk() {
    vector<string> path;
    unordered_set<string> visitedEdges;
    random_device rd;
    mt19937 gen(rd());
    if (nodes.empty()) return path;
    vector<string> vnodes(nodes.begin(), nodes.end());
    int maxIndex = static_cast<int>(vnodes.size()) - 1;
    uniform_int_distribution<int> dis(0, maxIndex);
    string cur = vnodes[dis(gen)];
    path.push_back(cur);
    while (true) {
        auto it = adjList.find(cur);
        if (it == adjList.end() || it->second.empty()) break;
        vector<string> dests;
        for (const auto& e : it->second) dests.push_back(e.first);
        int maxD = static_cast<int>(dests.size()) - 1;
        uniform_int_distribution<int> d2(0, maxD);
        string nxt = dests[d2(gen)];
        string edge = cur + "->" + nxt;
        if (!visitedEdges.insert(edge).second) break;
        path.push_back(nxt);
        cur = nxt;
    }
    return path;
}

int main() {
    string filename;
    cout << "Enter input text file: ";
    getline(cin >> std::ws, filename);
    auto words = tokenizeFile(filename);
    cout << "Tokenized words count: " << words.size() << endl;
    buildGraph(words);
    printGraph();
    while (true) {
        cout << "\nMenu:\n"
            << "1. Query bridge words\n"
            << "2. Generate new text\n"
            << "3. Shortest path\n"
            << "4. Compute PageRank\n"
            << "5. Random walk\n"
            << "6. Exit\n"
            << "Select option: ";
        int opt;
        if (!(cin >> opt)) break;
        if (opt == 6) break;
        switch (opt) {
        case 1: {
            string A, B;
            cout << "Enter A and B: "; cin >> A >> B;
            auto bridges = queryBridge(A, B);
            bool hasA = nodes.count(A), hasB = nodes.count(B);
            if (!hasA && !hasB) cout << "No " << A << " and " << B << " in the graph!\n";
            else if (!hasA) cout << "No " << A << " in the graph!\n";
            else if (!hasB) cout << "No " << B << " in the graph!\n";
            else if (bridges.empty()) cout << "No bridge words from " << A << " to " << B << "!\n";
            else if (bridges.size() == 1) cout << "The bridge word from " << A << " to " << B << " is: " << bridges[0] << ".\n";
            else {
                cout << "The bridge words from " << A << " to " << B << " are: ";
                for (size_t i = 0; i < bridges.size(); ++i) {
                    cout << bridges[i];
                    if (i + 1 < bridges.size()) cout << (i + 2 == bridges.size() ? " and " : ", ");
                }
                cout << ".\n";
            }
            break;
        }
        case 2: {
            cout << "Enter a new text file: ";
            string fn; getline(cin >> std::ws, fn);
            auto w2 = tokenizeFile(fn);
            cout << "Generated text:\n" << generateNewText(w2) << endl;
            break;
        }
        case 3: {
            string A, B;
            cout << "Enter start and end: ";
            cin >> A >> B;

            auto result = shortestPathWithWeight(A, B);
            auto& path = result.first;
            int totalWeight = result.second;

            if (path.empty()) {
                cout << "No path from " << A << " to " << B << "!\n";
            }
            else {
                // 生成高亮边集合
                unordered_set<string> highlightEdges;
                for (size_t i = 0; i < path.size() - 1; ++i) {
                    highlightEdges.insert(path[i] + "->" + path[i + 1]);
                }

                // 打印带高亮的图
                printGraph(highlightEdges);

                // 打印路径详情
                cout << "\nShortest Path Highlighted:" << endl;
                for (size_t i = 0; i < path.size(); ++i) {
                    cout << path[i];
                    if (i != path.size() - 1) cout << " -> ";
                }
                cout << "\nTotal path weight: " << totalWeight << endl;
            }
            break;
        }
        case 4: {
            auto pr = computePageRank();
            cout << "PageRank values:\n";
            for (const auto& p : pr) cout << p.first << ": " << p.second << "\n";
            break;
        }
        case 5: {
            cout << "Performing random walk...\n";
            auto walk = randomWalk();
            cout << "Walk path: ";
            for (size_t i = 0; i < walk.size(); ++i) cout << walk[i] << (i + 1 < walk.size() ? " -> " : "\n");
            ofstream fout("random_walk.txt");
            for (const auto& n : walk) fout << n << "\n";
            cout << "Saved to random_walk.txt\n";
            break;
        }
        default:
            cout << "Invalid option.\n";
        }
    }
    return 0;
}
