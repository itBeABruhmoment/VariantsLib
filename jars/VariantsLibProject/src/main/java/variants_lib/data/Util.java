package variants_lib.data;

import com.fs.starfarer.api.characters.PersonAPI;

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

    /**
     * A scuffed way of checking if a person is considered an officer (ie. you can see in a fleet preview screen) that
     * might be sufficient for the base game
     * @param person person to check
     * @return true if the person is an officer, false otherwise
     */
    public static boolean isOfficer(final PersonAPI person) {
        return person != null
                && person.getPortraitSprite() != null
                && !person.getPortraitSprite().equals("graphics/portraits/portrait_generic_grayscale.png");
    }
}
