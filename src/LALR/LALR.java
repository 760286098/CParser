package LALR;

import java.io.*;
import java.util.*;

public class LALR {
    private static final String strS = "S'";
    private static final String strEOF = "$";
    private static final String strEpsilon = "ε";
    private static String startSymbol;

    private ArrayList<Production> productions = new ArrayList<>();
    private ArrayList<String> vts = new ArrayList<>();
    private ArrayList<String> vns = new ArrayList<>();
    private HashMap<String, HashSet<String>> firsts = new HashMap<>();
    private HashMap<String, HashSet<String>> follows = new HashMap<>();

    private ArrayList<ArrayList<Project>> core_projects = new ArrayList<>();
    private ArrayList<ArrayList<Project>> statuses = new ArrayList<>();
    private HashMap<Map.Entry<Integer, String>, String> table = new HashMap<>();
    private ArrayList<Map.Entry<Integer, String>> conflicts = new ArrayList<>();
    private HashMap<Map.Entry<Integer, Integer>, String> positionOFR = new HashMap<>();
    private HashMap<Map.Entry<Integer, Integer>, HashSet<String>> spontaneous_symbol = new HashMap<>();
    private HashMap<Map.Entry<Integer, Integer>, HashSet<Map.Entry<Integer, Integer>>> transform = new HashMap<>();

    private LALR(String path) {
        Init(path);
        First();
        Follow();
        CreateDFA();
        GetLookahead();
        FillRWithLookahead();
        Show();
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        LALR lalr = new LALR("文法.txt");
        long endTime = System.currentTimeMillis();
        System.out.println("处理完成, 状态数目：" + lalr.statuses.size() + " , 冲突数目：" + lalr.conflicts.size() + " , 用时：" + (endTime - startTime) + "ms");
    }

    static String getStrEpsilon() {
        return strEpsilon;
    }

    private void Init(String path) {
        HashSet<String> allSymbol = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(path)));
            String s;
            int stage = 0;
            while ((s = br.readLine()) != null) {
                if (s.matches("\\s*")) {
                    stage++;
                    continue;
                }
                s = s.trim();
                if (stage == 0) {
                    for (String n : s.split("\\s+"))
                        if (!vns.contains(n))
                            vns.add(n);
                } else if (stage == 1) {
                    for (String n : s.split("\\s+"))
                        if (!vts.contains(n))
                            vts.add(n);
                } else {
                    String left = s.substring(0, s.indexOf("->"));
                    String right = s.substring(s.indexOf("->") + 2);
                    for (String string : right.split("\\$")) {
                        String[] rights = string.trim().split("\\s+");
                        if (rights[0].equals(strEpsilon)) {
                            rights = new String[0];
                        }
                        Production production = new Production(left, rights);
                        productions.add(production);
                        allSymbol.add(left);
                        allSymbol.addAll(Arrays.asList(rights));
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        allSymbol.removeAll(vns);
        allSymbol.removeAll(vts);
        if (!allSymbol.isEmpty()) {
            System.out.println("未知符号： " + allSymbol);
            System.exit(-1);
        }
        startSymbol = vns.get(0);
        productions.add(0, new Production(strS, new String[]{startSymbol}));

//        /* ****************************************压力测试****************************************** */
//        vns.add("S");
//        int n = 6; //2->24 3->77 4->254 5>867 6->2996 7->10313
//        for (int i = 1; i <= n; i++) {
//            vns.add("A" + i);
//            vns.add("B" + i);
//            vts.add("a" + i);
//            vts.add("b" + i);
//
//            productions.add(new Production("S", new String[]{"A" + i}));
//            productions.add(new Production("A" + i, new String[]{"a" + i, "B" + i}));
//            productions.add(new Production("A" + i, new String[]{"b" + i}));
//            productions.add(new Production("B" + i, new String[]{"b" + i}));
//
//            for (int j = 1; j <= n; j++) {
//                if (i != j) {
//                    productions.add(new Production("A" + i, new String[]{"a" + j, "A" + i}));
//                    productions.add(new Production("B" + i, new String[]{"a" + j, "B" + i}));
//                }
//            }
//        }
//        startSymbol = vns.get(0);
//        productions.add(0, new Production(strS, new String[]{startSymbol}));
//        /* ****************************************压力测试****************************************** */
    }

    private void First() {
        for (String vn : vns) {
            HashSet<String> firstSet = new HashSet<>();
            firsts.put(vn, firstSet);
        }

        boolean isChanged;
        do {
            isChanged = false;
            for (String vn : vns) {
                for (Production production : productions) {
                    if (production.getLeft().equals(vn)) {
                        isChanged |= firsts.get(vn).addAll(getFirst(production.getRight(), 0));
                    }
                }
            }
        } while (isChanged);
        firsts.put(strS, firsts.get(startSymbol));
    }

    private void Follow() {
        HashSet<String> initialFollow = new HashSet<>();
        initialFollow.add(strEOF);
        follows.put(strS, initialFollow);

        for (String vn : vns) {
            HashSet<String> followSet = new HashSet<>();
            follows.put(vn, followSet);
        }

        boolean isChanged;
        do {
            isChanged = false;
            for (String vn : vns) {
                for (Production production : productions) {
                    String[] right = production.getRight();
                    for (int i = 0; i < right.length; i++) {
                        if (right[i].equals(vn)) {
                            if (i == right.length - 1) {
                                isChanged |= follows.get(vn).addAll(follows.get(production.getLeft()));
                            } else {
                                HashSet<String> tmp = getFirst(right, i + 1);
                                if (tmp.contains(strEpsilon)) {
                                    tmp.remove(strEpsilon);
                                    tmp.addAll(follows.get(production.getLeft()));
                                }
                                isChanged |= follows.get(vn).addAll(tmp);
                            }
                        }
                    }
                }
            }
        } while (isChanged);
    }

    private HashSet<String> getFirst(String[] right, int index) {
        HashSet<String> first = new HashSet<>();
        if (index == right.length) {
            if (index == 0) {
                first.add(strEpsilon);
            }
            return first;
        }

        String s = right[index];
        if (isVt(s)) {
            first.add(s);
            return first;
        }
        if (isVn(s)) {
            first.addAll(firsts.get(s));
        }
        if (first.contains(strEpsilon)) {
            if (index != right.length - 1) {
                first.addAll(getFirst(right, index + 1));
                first.remove(strEpsilon);
            }
        }
        return first;
    }

    private void CreateDFA() {// 建立dfa
        ArrayList<Project> projects = new ArrayList<>();
        projects.add(new Project(productions.get(0), 0));
        core_projects.add(projects);

        for (int i = 0; i < core_projects.size(); i++) {
            projects = closure(core_projects.get(i));
            for (Project project : projects) {
                String nextString = project.getNextString();
                if (nextString != null) {// shift项目或者goto项目
                    ArrayList<Project> tmp = goTo(projects, nextString);
                    if (!core_projects.contains(tmp)) {
                        fillTable(i, nextString, String.valueOf(core_projects.size()));
                        core_projects.add(tmp);
                    } else {
                        fillTable(i, nextString, String.valueOf(core_projects.indexOf(tmp)));
                    }
                } else {// 如果dot在句尾，则是reduce项目
                    if (project.getProduction().getLeft().equals(strS)) {
                        fillTable(i, strEOF, "acc");// i行EOF列为acc
                    } else {
                        positionOFR.put(new AbstractMap.SimpleEntry<>(i, projects.indexOf(project)), "r" + productions.indexOf(project.getProduction()));
                    }
                }
            }
        }
    }

    private ArrayList<Project> closure(ArrayList<Project> projects) {// 获取闭包
        ArrayList<Project> result = new ArrayList<>(projects);
        boolean isChanged;
        do {
            isChanged = false;
            for (int i = 0; i < result.size(); i++) {
                String nextString = result.get(i).getNextString();
                if (isVn(nextString)) {
                    for (Production production : productions) {
                        Project newProject = new Project(production, 0);
                        if (production.getLeft().equals(nextString) && !result.contains(newProject)) {
                            result.add(newProject);
                            isChanged = true;
                        }
                    }
                }
            }
        } while (isChanged);
        return result;
    }

    private ArrayList<Project> goTo(ArrayList<Project> items, String s) {// 求items中每个project遇到文法符号s的下个状态
        ArrayList<Project> result = new ArrayList<>();
        for (Project item : items) {
            if (s.equals(item.getNextString())) {
                result.add(item.getNextProject());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void GetLookahead() {
        for (int i = 0; i < core_projects.size(); i++) {
            ArrayList<Project> projects = core_projects.get(i);
            for (int j = 0; j < projects.size(); j++) {
                Project project = projects.get(j);
                if (project.getNextString() != null) {
                    ArrayList<Project> closure = closureWithLookahead(project);
                    for (Project item : closure) {
                        if (item.getNextString() != null) {
                            HashSet<String> lookahead = item.getLookahead();
                            String s = lookupTable(i, item.getNextString());
                            if (s.startsWith("s")) {
                                s = s.substring(1);
                            }
                            int indexOfProjects = Integer.parseInt(s);
                            int indexOfProject = core_projects.get(indexOfProjects).indexOf(item.getNextProject());
                            if (lookahead.contains(strEOF)) {
                                Map.Entry<Integer, Integer> entry = new AbstractMap.SimpleEntry<>(i, j);
                                Map.Entry<Integer, Integer> value = new AbstractMap.SimpleEntry<>(indexOfProjects, indexOfProject);
                                HashSet<Map.Entry<Integer, Integer>> hashSet = transform.get(entry);
                                if (hashSet == null) {
                                    hashSet = new HashSet<>();
                                    hashSet.add(value);
                                    transform.put(entry, hashSet);
                                } else {
                                    hashSet.add(value);
                                }
                            }
                            lookahead = (HashSet<String>) lookahead.clone();
                            lookahead.remove(strEOF);
                            if (!lookahead.isEmpty()) {
                                Map.Entry<Integer, Integer> entry = new AbstractMap.SimpleEntry<>(indexOfProjects, indexOfProject);
                                HashSet<String> hashSet = spontaneous_symbol.get(entry);
                                if (hashSet == null) {
                                    hashSet = new HashSet<>(lookahead);
                                    spontaneous_symbol.put(entry, hashSet);
                                } else {
                                    hashSet.addAll(lookahead);
                                }
                            }
                        }
                    }
                }
                project.setLookahead(new HashSet<>());
            }
        }
    }

    private ArrayList<Project> closureWithLookahead(Project sourceProject) {
        ArrayList<Project> projects = new ArrayList<>();
        sourceProject.getLookahead().add(strEOF);
        projects.add(sourceProject);
        return closureWithLookahead(projects);
    }

    private ArrayList<Project> closureWithLookahead(ArrayList<Project> projects) {
        ArrayList<Project> result = new ArrayList<>(projects);
        boolean isChanged;
        do {
            isChanged = false;
            for (int i = 0; i < result.size(); i++) {
                Project project = result.get(i);
                if (isVn(project.getNextString())) {
                    HashSet<String> lookahead = new HashSet<>();
                    String string = project.getStringOfNextString();
                    if (string == null) {
                        lookahead.addAll(project.getLookahead());
                    } else if (isVn(string)) {
                        HashSet<String> firstOfString = firsts.get(string);
                        lookahead.addAll(firstOfString);
                        if (firstOfString.contains(strEpsilon)) {
                            lookahead.addAll(project.getLookahead());
                            lookahead.remove(strEpsilon);
                        }
                    } else if (isVt(string)) {
                        lookahead.add(string);
                    }

                    for (Production production : productions) {
                        if (production.getLeft().equals(project.getNextString())) {
                            Project newProject = new Project(production, 0);
                            if (result.contains(newProject)) {
                                isChanged |= result.get(result.indexOf(newProject)).getLookahead().addAll(lookahead);
                            } else {
                                newProject.setLookahead(lookahead);
                                result.add(newProject);
                                isChanged = true;
                            }
                        }
                    }
                }
            }
        } while (isChanged);
        return result;
    }

    private void FillRWithLookahead() {
        core_projects.get(0).get(0).getLookahead().add(strEOF);
        for (Map.Entry<Integer, Integer> key : spontaneous_symbol.keySet()) {
            core_projects.get(key.getKey()).get(key.getValue()).getLookahead().addAll(spontaneous_symbol.get(key));
        }

        boolean isChanged;
        do {
            isChanged = false;
            for (Map.Entry<Integer, Integer> key : transform.keySet()) {
                HashSet<Map.Entry<Integer, Integer>> hashSet = transform.get(key);
                for (Map.Entry<Integer, Integer> value : hashSet) {
                    isChanged |= core_projects.get(value.getKey()).get(value.getValue()).getLookahead().addAll(core_projects.get(key.getKey()).get(key.getValue()).getLookahead());
                }
            }
        } while (isChanged);

        for (ArrayList<Project> projects : core_projects) {
            statuses.add(closureWithLookahead(projects));
        }

        for (Map.Entry<Integer, Integer> key : positionOFR.keySet()) {
            Project project = statuses.get(key.getKey()).get(key.getValue());
//            for (String vt : vts) {// LR(0)
//                fillTable(key.getKey(), vt, positionOFR.get(key));
//            }
//            for (String follow : follows.get(project.getProduction().getLeft())) {// SLR(1)
//                fillTable(key.getKey(), follow, positionOFR.get(key));
//            }
            for (String lookahead : project.getLookahead()) {// LALR(1)
                fillTable(key.getKey(), lookahead, positionOFR.get(key));
            }
        }
    }

    private void Show() {
        ArrayList<String> header = new ArrayList<>(vts);
        header.add(strEOF);
        header.addAll(vns);

        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("LALRTabble.txt")));
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("LALRTabble"));

            int size_productions = productions.size();
            for (int i = 0; i < size_productions; i++) {
                writer.println(i + "    " + productions.get(i));
            }
            writer.println();

            int size_statuses = statuses.size();
            for (int i = 0; i < size_statuses; i++) {
                writer.println(i + "    " + statuses.get(i));
            }
            writer.println();

            int size_vn = vns.size();
            for (int i = 0; i < size_vn; i++) {
                writer.println(i + "  " + vns.get(i) + "  " + firsts.get(vns.get(i)));
            }
            writer.println();

            for (int i = 0; i < size_vn; i++) {
                writer.println(i + "  " + vns.get(i) + "  " + follows.get(vns.get(i)));
            }
            writer.println();

            String format = "%-30s";
            writer.print(String.format(format, "-"));
            for (String s : header)
                writer.print(String.format(format, s));
            writer.println();

            for (int i = 0; i < size_statuses; i++) {
                writer.print(String.format(format, i));
                for (String s : header) {
                    String value = lookupTable(i, s);
                    writer.print(String.format(format, value == null ? "-" : value));
                }
                writer.println();
            }
            writer.println();

            writer.println("conflicts size: " + conflicts.size());
            for (Map.Entry<Integer, String> entry : conflicts) {
                String value = table.get(entry);
                writer.println(entry.getValue() + "    " + entry.getKey() + "   " + value);

                if (value.startsWith("r")) {
                    value = value.substring(value.indexOf("s"));
                } else {
                    value = value.substring(0, value.indexOf("r"));
                }
                table.put(entry, value);
            }
            writer.flush();

            oos.writeObject(vts);
            oos.writeObject(table);
            for (Production production : productions)
                oos.writeObject(production.getLeft() + ":" + production.getRight().length);
            oos.writeObject(null);
            writer.close();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isVt(String s) {
        return vts.contains(s);
    }

    private boolean isVn(String s) {
        return vns.contains(s);
    }

    private void fillTable(int row, String column, String value) {
        if (isVt(column) && !value.startsWith("r")) {
            value = "s" + value;
        }

        String tmp = lookupTable(row, column);
        if (tmp == null) {
            table.put(new AbstractMap.SimpleEntry<>(row, column), value);
        } else if (!tmp.contains(value)) {
            table.put(new AbstractMap.SimpleEntry<>(row, column), tmp + value);
            conflicts.add(new AbstractMap.SimpleEntry<>(row, column));
        }
    }

    private String lookupTable(int row, String column) {
        return table.get(new AbstractMap.SimpleEntry<>(row, column));
    }
}