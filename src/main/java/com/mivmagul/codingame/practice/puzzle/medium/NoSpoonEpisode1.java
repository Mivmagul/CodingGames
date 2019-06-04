package com.mivmagul.codingame.practice.puzzle.medium;

import java.util.*;

class NoSpoonEpisode1 {
    private static List<Node> nodes = new ArrayList<>();

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int width = in.nextInt();
        int height = in.nextInt();
        in.nextLine();

        for (int i = 0; i < height; i++) {
            String line = in.nextLine();
            for (int j = 0; j < line.length(); j++) {
                if (line.charAt(j) == '0') {
                    nodes.add(new Node(j ,i));
                }
            }
        }
        nodes.forEach(node -> System.out.println(node + " " + getRightNode(node) + " " + getDownNode(node)));
    }

    private static Node getDownNode(Node node) {
        return nodes.stream()
                .filter(n -> n.getX() == node.getX())
                .filter(n -> n.getY() > node.getY())
                .reduce((n1, n2) -> n1.getY() < n2.getY() ? n1 : n2)
                .orElse(new Node(-1, -1));
    }

    private static Node getRightNode(Node node) {
        return nodes.stream()
                .filter(n -> n.getY() == node.getY())
                .filter(n -> n.getX() > node.getX())
                .reduce((n1, n2) -> n1.getX() < n2.getX() ? n1 : n2)
                .orElse(new Node(-1, -1));
    }

    static class Node {
        private int x, y;

        public Node(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public String toString() {
            return x + " " + y;
        }
    }
}