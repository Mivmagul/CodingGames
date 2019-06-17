package com.mivmagul.codingame.practice.puzzle.medium;

import java.util.*;

class TheGift {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int participants = in.nextInt();
        int price = in.nextInt();
        int[] budgets = new int[participants];
        for (int i = 0; i < participants; i++) {
            budgets[i] = in.nextInt();
        }

        if (Arrays.stream(budgets).sum() < price) {
            System.out.println("IMPOSSIBLE");
        } else {
            Arrays.sort(budgets);

            int[] result = new int[participants];
            int k = 0;
            for (int budget : budgets) {
                int neededPart = price / participants;

                if (budget < neededPart) {
                    result[k++] = budget;
                    price -= budget;
                } else {
                    if (price % participants > 0) {
                        neededPart++;
                    }
                    result[k++] = neededPart;
                    price -= neededPart;
                }
                participants--;
            }

            Arrays.stream(result).sorted().forEach(System.out::println);
        }
    }
}
