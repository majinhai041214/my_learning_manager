package com.majinhai.website.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.majinhai.website.config.StorageProperties;
import com.majinhai.website.exception.BusinessException;
import com.majinhai.website.model.entity.AlgorithmRoadmap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class FileAlgorithmRoadmapRepository implements AlgorithmRoadmapRepository {

    private final ObjectMapper objectMapper;
    private final Path metadataFile;
    private AlgorithmRoadmap roadmap;

    public FileAlgorithmRoadmapRepository(StorageProperties storageProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.metadataFile = Path.of(storageProperties.getBaseDir(), storageProperties.getRoadmapFile());
        loadExistingData();
    }

    @Override
    public synchronized AlgorithmRoadmap get() {
        return copy(roadmap);
    }

    @Override
    public synchronized AlgorithmRoadmap save(AlgorithmRoadmap updatedRoadmap) {
        roadmap = copy(updatedRoadmap);
        persist();
        return copy(roadmap);
    }

    private void loadExistingData() {
        try {
            Files.createDirectories(metadataFile.getParent());

            if (Files.notExists(metadataFile) || Files.size(metadataFile) == 0L) {
                roadmap = createDefaultRoadmap();
                persist();
                return;
            }

            roadmap = objectMapper.readValue(metadataFile.toFile(), AlgorithmRoadmap.class);
        } catch (IOException exception) {
            throw new BusinessException("ROADMAP_INIT_FAILED", "初始化算法路线图存储失败");
        }
    }

    private void persist() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), roadmap);
        } catch (IOException exception) {
            throw new BusinessException("ROADMAP_WRITE_FAILED", "写入算法路线图存储失败");
        }
    }

    private AlgorithmRoadmap copy(AlgorithmRoadmap source) {
        return objectMapper.convertValue(source, AlgorithmRoadmap.class);
    }

    private AlgorithmRoadmap createDefaultRoadmap() {
        AlgorithmRoadmap defaultRoadmap = new AlgorithmRoadmap();
        defaultRoadmap.setTitle("算法学习主路线");
        defaultRoadmap.setSubtitle("从基础到专题，再到综合能力");
        defaultRoadmap.setMilestones(List.of(
                "第一阶段：先完成基础能力和搜索回溯，建立刷题节奏。",
                "第二阶段：把动态规划和图论作为主线专题持续推进。",
                "第三阶段：再补字符串、数学和综合题，形成稳定题感。"
        ));

        defaultRoadmap.setBranches(List.of(
                createBranch("branch-basic", "基础能力", "cyan", "先把最常用的数据结构、复杂度思维和基础技巧练顺。",
                        List.of(
                                createGroup("group-basic-structure", "数据结构", List.of("数组与字符串", "链表", "栈与队列", "哈希表", "双指针")),
                                createGroup("group-basic-skill", "基础技巧", List.of("前缀和与差分", "二分答案", "排序与自定义比较", "模拟与构造"))
                        )),
                createBranch("branch-search", "搜索与递归", "gold", "把状态树看清楚，能区分暴搜、剪枝和图搜索的适用场景。",
                        List.of(
                                createGroup("group-search-framework", "搜索框架", List.of("DFS", "BFS", "递归回溯", "连通块搜索", "最短步数模型")),
                                createGroup("group-search-topics", "典型专题", List.of("排列组合", "子集枚举", "棋盘搜索", "剪枝优化"))
                        )),
                createBranch("branch-dp", "动态规划", "emerald", "围绕状态设计、转移方程和空间优化建立稳定模板。",
                        List.of(
                                createGroup("group-dp-basic", "入门模型", List.of("线性 DP", "背包问题", "最长上升子序列", "区间 DP")),
                                createGroup("group-dp-advanced", "进阶方向", List.of("树形 DP", "状态压缩 DP", "计数 DP", "优化转移"))
                        )),
                createBranch("branch-graph", "图论体系", "violet", "把图的表示、遍历和最短路/拓扑/并查集串成一条完整路线。",
                        List.of(
                                createGroup("group-graph-basic", "基础图算法", List.of("邻接表建图", "拓扑排序", "并查集", "最短路", "最小生成树")),
                                createGroup("group-graph-advanced", "专题延展", List.of("强连通分量", "二分图", "树与最近公共祖先", "网络流认识"))
                        )),
                createBranch("branch-string", "字符串与数学", "rose", "适合作为中后期提效方向，把模板和常见套路沉淀下来。",
                        List.of(
                                createGroup("group-string", "字符串", List.of("KMP", "Trie", "字符串哈希", "字典树检索")),
                                createGroup("group-math", "数学与技巧", List.of("位运算", "组合计数", "快速幂", "数论基础"))
                        ))
        ));

        return defaultRoadmap;
    }

    private AlgorithmRoadmap.RoadmapBranch createBranch(
            String id,
            String title,
            String tone,
            String summary,
            List<AlgorithmRoadmap.RoadmapGroup> groups
    ) {
        AlgorithmRoadmap.RoadmapBranch branch = new AlgorithmRoadmap.RoadmapBranch();
        branch.setId(id);
        branch.setTitle(title);
        branch.setTone(tone);
        branch.setSummary(summary);
        branch.setGroups(groups);
        return branch;
    }

    private AlgorithmRoadmap.RoadmapGroup createGroup(String id, String name, List<String> items) {
        AlgorithmRoadmap.RoadmapGroup group = new AlgorithmRoadmap.RoadmapGroup();
        group.setId(id);
        group.setName(name);
        group.setItems(items);
        return group;
    }
}
