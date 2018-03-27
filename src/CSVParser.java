/**
 * Created by isuca in work catalogue
 *
 * @date 01-Oct-17
 * @time 23:41
 */

import javax.swing.tree.TreeNode;
import java.io.*;
import java.util.*;

class CSVParser {

    // Input/Output file reader/writer
    private final IO files;

    // List of all "rule" elements
    private ArrayList<List<String>> elements;
    // How types and nodes correspond with each other
    private HashMap<String, TreeNode> types;
    // Number of last row read
    private int last;

    /**
     * Constructor with paths to input/output files
     *
     * @param pathToInput  path to input (.csv format)
     * @param pathToOutput path to output (.json format)
     */
    CSVParser(String pathToInput, String pathToOutput) {
        files = new IO(pathToInput, pathToOutput);

        elements = new ArrayList<>();
        last = 0;
    }

    /**
     * Closes output file
     */
    void close() {
        files.out.close();
    }

    /**
     * Converts all values from .csv file into tree representing rules of tag ordering
     *
     * @return root of the tree
     */
    TreeNode convertFromCSV() {
        String line = files.nextLine();
        while (line != null) {
            elements.add(parseLine(line));
            line = files.nextLine();
        }

        return buildTree();
    }

    /**
     * Prints all the data to .json from the selected root (debug needs only)
     *
     * @param root root of .csv tree
     */
    void printToJSON(TreeNode root) {
        files.out.print(printToJSON(root, "", ""));
        files.out.println("}");
    }

    /**
     * Prints current node and all lower nodes data to .json (debug needs only)
     *
     * @param node  current node
     * @param path  path to current node in .xml file
     * @param level depth of current node in the tree
     * @return string representation of all data
     */
    private String printToJSON(TreeNode node, String path, String level) {
        StringBuilder out = new StringBuilder();
        out.append(level).append("{\n");
        path = path + '/' + node.path;

        level += "  ";
        out.append(level).append("\"path\": \"").append(path).append("\",\n");
        out.append(level).append("\"name\": \"").append(node.name.charAt(0) == '"' ? node.name.substring(1) : node.name).append("\"");
        if (node.node) {
            out.append(',');
        }
        out.append('\n');
        if (node.node) {
            out.append(level).append("\"params\": [\n");
            for (int i = 0; i < node.params.size(); i++) {
                out.append(printToJSON(node.params.get(i), path, level + "  "));
                if (i < node.params.size() - 1) {
                    out.append(",");
                }
                out.append('\n');
            }
            out.append(level).append("],\n");

            out.append(level).append("\"children\": [\n");
            for (int i = 0; i < node.children.size(); i++) {
                out.append(printToJSON(node.children.get(i), path, level + "  "));
                if (i < node.children.size() - 1) {
                    out.append(",");
                }
                out.append('\n');
            }
            out.append(level).append("]\n");
        }
        level = level.substring(0, level.length() - 2);
        out.append(level).append("}");

        return String.valueOf(out);
    }

    /**
     * Creates tree from .csv.file
     *
     * @return root of the tree
     */
    private TreeNode buildTree() {
        types = new HashMap<>();
        TreeNode root = new TreeNode("Response", "root", true, true);
        while (last < elements.size()) {
            TreeNode next = parseNode();
            if (next.node) {
                root.children.add(next);
            } else {
                root.params.add(next);
            }
        }

        return root;
    }

    /**
     * Extracts type name (if present) based on description in the row
     *
     * @param desc description string
     * @return type name or "" if not present
     */
    private String getTypeDesc(String desc) {
        try {
            return desc.substring(desc.indexOf('<'), desc.indexOf('>') + 1);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * Creates node from the next string in input file
     *
     * @return node
     */
    private TreeNode parseNode() {
        List<String> node = elements.get(last++);
        int level = getLevel(last - 1);
        boolean isNode = node.get(4).length() > 0 && node.get(4).charAt(0) == 'С', required = node.get(3).length() > 0 && node.get(3).charAt(0) == 'О';
        String nodeType = getTypeDesc(node.get(5));

        TreeNode cur = new TreeNode(node.get(1), node.get(2), isNode, required);
        if (isNode) {
            // Parses all children of this node
            while (last < elements.size() && getLevel(last) == level + 1) {
                TreeNode next = parseNode();
                if (next.node) {
                    cur.children.add(next);
                } else {
                    cur.params.add(next);
                }
            }
        }

        // Corrects node if it's type was declared before
        if (!Objects.equals(nodeType, "")) {
            cur.type = nodeType;
            if (types.containsKey(nodeType)) {
                if (cur.children.size() == 0 && cur.params.size() == 0) {
                    TreeNode template = types.get(nodeType);
                    cur.params.addAll(template.params);
                    cur.children.addAll(template.children);
                }
            } else {
                types.put(nodeType, cur);
            }
        }
        return cur;
    }

    /**
     * Returns depth of the node based number in the row
     * @param idx index of the row
     * @return level
     */
    private int getLevel(int idx) {
        return elements.get(idx).get(0).split("\\.").length - 1;
    }

    /**
     * Parses line from .csv file into suitable format
     * @param csvLine .csv file line
     * @return list of strings in this line
     */
    private static List<String> parseLine(String csvLine) {
        List<String> result = new ArrayList<>();
        if (csvLine == null || csvLine.isEmpty()) {
            return result;
        }

        StringBuffer curVal = new StringBuffer();
        boolean inQuotes = false;
        boolean startCollectChar = false;
        char[] chars = csvLine.toCharArray();

        for (char ch : chars) {
            if (inQuotes) {
                startCollectChar = true;
                if (ch == '"') {
                    inQuotes = false;
                } else {
                    curVal.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                    if (startCollectChar) {
                        curVal.append('"');
                    }
                } else if (ch == ',') {
                    result.add(curVal.toString());
                    curVal = new StringBuffer();
                    startCollectChar = false;
                } else if (ch == '\n') {
                    break;
                } else if (ch != '\r') {
                    curVal.append(ch);
                }
            }
        }

        result.add(curVal.toString());
        return result;
    }

    /**
     * Node class which represents one row in .csv file
     */
    class TreeNode {
        // path in .xml, name in .pdf, type name
        private String path, name, type;
        // is tag or not, is required or not
        private boolean node, req;
        // list of parameters, list of children
        private ArrayList<TreeNode> params, children;

        TreeNode(String path, String name, boolean node, boolean req) {
            this.path = path;
            this.name = name;
            type = "";
            this.node = node;
            this.req = req;
            params = new ArrayList<>();
            children = new ArrayList<>();
        }

        String getPath() {
            return path;
        }

        String getName() {
            return name;
        }

        boolean isNode() {
            return node;
        }

        boolean isRequired() {
            return req;
        }

        ArrayList<TreeNode> getParams() {
            return params;
        }

        ArrayList<TreeNode> getChildren() {
            return children;
        }

        String getType() {
            return type;
        }
    }

    // Class that allows fast read from input and fast print to output
    private class IO {
        StringTokenizer st;
        BufferedReader in;
        PrintWriter out;

        IO() {
            in = new BufferedReader(new InputStreamReader(System.in));
            out = new PrintWriter(System.out);
        }

        IO(String in, String out) {
            try {
                this.in = new BufferedReader(new FileReader(in));
                this.out = new PrintWriter(out);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        String nextLine() {
            try {
                return in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

    }

}
