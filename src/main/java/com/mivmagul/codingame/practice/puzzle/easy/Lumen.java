package com.mivmagul.codingame.practice.puzzle.easy;

import java.util.Scanner;

class Lumen {
    private static String[][] array;
    private static int illumination;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int roomLength = in.nextInt();
        illumination = in.nextInt();

        in.nextLine();

        array = new String[roomLength][roomLength];

        for (int index = 0; index < array.length; index++) {
            array[index] = in.nextLine().split(" ");
        }

        int result = 0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array.length; j++) {
                if (array[i][j].equals("X")) {
                    if (!isIlluminated(i, j)) {
                        result++;
                    }
                }
            }
        }
        System.out.println(result);
    }

    private static boolean isIlluminated(int i, int j) {
        for (int m = getFrom(i); m < getTo(i); m++) {
            for (int n = getFrom(j); n < getTo(j); n++) {
                if (array[m][n].equals("C")){
                    return true;
                }
            }
        }
        return false;
    }

    private static int getFrom(int index) {
        return index - illumination + 1 > 0
                ? index - illumination + 1
                : 0;
    }

    private static int getTo(int index) {
        return index + illumination < array.length
                ? index + illumination
                : array.length;
    }
}