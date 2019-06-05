package com.mivmagul.codingame.practice.puzzle.medium;

import java.util.*;
import java.util.stream.IntStream;

class MayanCalculation {
    private static final int NUMERALS_AMOUNT = 20;

    private static Scanner in = new Scanner(System.in);
    private static int width, height;
    private static List<List<String>> mayanNumerals = new ArrayList<>(NUMERALS_AMOUNT);

    public static void main(String[] args) {
        width = in.nextInt();
        height = in.nextInt();
        for (int i = 0; i < height; i++) {
            String numeral = in.next();
            for (int j = 0; j < NUMERALS_AMOUNT; j++) {
                String substring = numeral.substring(j * width, j * width + width);
                if (mayanNumerals.size() > j) {
                    mayanNumerals.get(j).add(substring);
                } else {
                    mayanNumerals.add(new ArrayList<>(height){{add(substring);}});
                }
            }
        }

        long firstNumber = readNumber();
        long secondNumber = readNumber();
        String operation = in.next();

        long result = calculate(firstNumber, operation, secondNumber);
        decodeNumeral(result).stream().flatMap(numeral -> mayanNumerals.get(Math.toIntExact(numeral)).stream()).forEach(System.out::println);
    }

    private static long readNumber() {
        int linesAmount = in.nextInt();
        List<Long> numerals = new ArrayList<>(linesAmount/height);
        List<String> currentMayanNumeral = new ArrayList<>(height);
        for (int i = 0; i < linesAmount; i++) {
            currentMayanNumeral.add(in.next());
            if ((i + 1) % height == 0) {
                numerals.add(0, (long) mayanNumerals.indexOf(currentMayanNumeral));
                currentMayanNumeral.clear();
            }
        }
        return encodeNumeral(numerals);
    }

    private static long encodeNumeral(List<Long> numerals) {
        return IntStream.range(0, numerals.size()).mapToLong(i -> (long) (numerals.get(i) * Math.pow(NUMERALS_AMOUNT, i))).sum();
    }

    private static List<Long> decodeNumeral(long numeral) {
        List<Long> numerals = new ArrayList<>();
        decodeNumeral(numeral, numerals);
        return numerals;
    }

    private static void decodeNumeral(long numeral, List<Long> numerals) {
        numerals.add(0, numeral % NUMERALS_AMOUNT);
        if (numeral / NUMERALS_AMOUNT > 0) {
            decodeNumeral(numeral / NUMERALS_AMOUNT, numerals);
        }
    }

    private static long calculate(long firstNumber, String operation, long secondNumber) {
        switch (operation) {
            case "*":
                return firstNumber * secondNumber;
            case "/":
                return firstNumber / secondNumber;
            case "+":
                return firstNumber + secondNumber;
            case "-":
                return firstNumber - secondNumber;
            default: return 0;
        }
    }
}
