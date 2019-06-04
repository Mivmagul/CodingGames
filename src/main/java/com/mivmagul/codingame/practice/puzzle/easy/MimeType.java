package com.mivmagul.codingame.practice.puzzle.easy;

import java.util.*;

class MimeType {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        int elementsAmount = in.nextInt();
        int fileNamesAmount = in.nextInt();

        Map<String, String> ext2mime = new HashMap<>();
        for (int i = 0; i < elementsAmount; i++) {
            ext2mime.put(in.next().toLowerCase(), in.next());
        }

        in.nextLine(); // extra line input :)

        for (int i = 0; i < fileNamesAmount; i++) {
            String fileName = in.nextLine();
            int beginIndex = fileName.lastIndexOf('.');

            System.out.println(
                    beginIndex < 0
                            ? "UNKNOWN"
                            : ext2mime.getOrDefault(fileName.substring(beginIndex + 1).toLowerCase(), "UNKNOWN")
            );
        }

    }
}