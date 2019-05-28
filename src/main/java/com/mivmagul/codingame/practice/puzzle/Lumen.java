package com.mivmagul.codingame.practice.puzzle;

import java.util.Scanner;

class Lumen {
    static String[][] array;
    static int illumination;

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

/*
THEY put you in a square shape room, with N meters on each side.
THEY want to know everything about you.
THEY are observing you.
THEY placed some candles in the room.

Every candle makes L "light" in the spot they are, and every spot in square shape gets one less "light" as the next ones. If a spot is touched by two candles, it will have the larger "light" it can have. Every spot has the base light of 0.

You can hide only, if you find a dark spot which has 0 "light".
How many dark spots you have?

You will receive a map of the room, with the empty places (X) and Candles (C) in N rows, each character separated by a space.

Example for the light spread N = 5, L = 3:
X X X X X
X C X X X
X X X X X
X X X X X
X X X X X

2 2 2 1 0
2 3 2 1 0
2 2 2 1 0
1 1 1 1 0
0 0 0 0 0
Input
Line 1: An integer N for the length of one side of the room.
Line 2: An integer L for the base light of the candles.
Next N lines: N number of characters (as c), separated by one space.
Output
Line 1 : The number of places with zero light.
Constraints
0 < N <= 25
0 < L < 10
Example
Input
5
3
X X X X X
X C X X X
X X X X X
X X X X X
X X X X X
Output
9
*/