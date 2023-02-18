package variants_lib.data;

import java.util.Random;

public class Util {
    public static int[] createRandomNumberSequence(int length, Random rand) {
        final int[] sequence = new int[length];
        for(int i = 0; i < sequence.length; i++) {
            sequence[i] = i;
        }

        // shuffle
        for (int i = sequence.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            // Simple swap
            int temp = sequence[index];
            sequence[index] = sequence[i];
            sequence[i] = temp;
        }
        return sequence;
    }
}
