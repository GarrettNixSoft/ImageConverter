package ImageMerger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

public class ImageMerger {

    public static void main(String[] args) {
        
        if (args.length < 1) {
            System.out.println("Usage: <file 1> <file 2> <dest file> <merge mode>");
            System.exit(0);
        }

        else if (args[0].contains("-help")) {
            System.out.println("Usage: <file 1> <file 2> <dest file> <merge mode>");
            System.out.println("Modes:");
            System.out.println("\t0: keep brightest");
            System.exit(0);
        }

        else if (args.length < 4 || args.length > 4) {
            System.out.println("Usage: <file 1> <file 2> <dest file> <merge mode>");
            System.exit(0);
        }

        String path1Str = args[0];
        String path2Str = args[1];
        String destStr = args[2];
        String modeStr = args[3];

        // parse mode
        int mode = -1;
        try {
            mode = Integer.parseInt(modeStr);
            if (mode < 0 || mode > 0) throw new IllegalArgumentException();
        }
        catch (NumberFormatException e) {
            System.err.println("Error parsing mode: please enter a valid integer");
            System.exit(-1);
        }
        catch (IllegalArgumentException iae) {
            System.err.println("Error parsing mode: please enter a valid mode. See \"ImageMerger -help\" for available modes");
            System.exit(-1);
        }

        // get files
        Path path1 = Paths.get(path1Str);
        Path path2 = Paths.get(path2Str);

        File file1 = path1.toFile();
        File file2 = path2.toFile();

        if (!file1.exists()) {
            System.err.println("Error: file1 does not exist");
            System.exit(-1);
        }
        if (!file2.exists()) {
            System.err.println("Error: file2 does not exist");
            System.exit(-1);
        }

        // get destination file
        Path destPath = Paths.get(destStr);
        File destFile = destPath.toFile();

        if (destFile.exists()) {
            System.err.println("Destination file already exists!");
            System.exit(-1);
        }
        
        try {
            destFile.createNewFile();
        }
        catch (IOException e) {
            System.err.println("Error creating dest file: " + e.getMessage());
            System.exit(-1);
        }

        // load images
        BufferedImage image1 = null;
        BufferedImage image2 = null;

        try {
            image1 = ImageIO.read(file1);
        }
        catch (Exception e) {
            System.err.println("Error loading image1: " + e.getMessage());
            System.exit(-1);
        }

        try {
            image2 = ImageIO.read(file2);
        }
        catch (Exception e) {
            System.err.println("Error loading image2: " + e.getMessage());
            System.exit(-1);
        }

        // confirm image dimensions match
        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            System.err.println("Error: images bust be the same size!");
            System.exit(-1);
        }

        // perform merge
        BufferedImage result = switch (mode) {
            case 0 -> mergeImagesKeepBrightest(image1, image2);
            default -> null;
        };

        // save the result to a file
        try {
            ImageIO.write(result, "png", destFile);
        }
        catch (IOException e) {
            System.err.println("Error saving result: " + e.getMessage());
            System.exit(-1);
        }

        // success!
        System.out.println("Success!");

    }

    private static BufferedImage mergeImagesKeepBrightest(BufferedImage image1, BufferedImage image2) {
        // create result image
        BufferedImage result = new BufferedImage(image1.getWidth(), image1.getHeight(), BufferedImage.TYPE_INT_ARGB);
        // merge
        for (int x = 0; x < result.getWidth(); x++) {
            for (int y = 0; y < result.getHeight(); y++) {
                // get pixel values
                int pixel1 = image1.getRGB(x, y);
                int pixel2 = image2.getRGB(x, y);
                // use a bit mask to get the alpha only
                int pixel1Alpha = pixel1 & 0xFF000000;
                int pixel2Alpha = pixel2 & 0xFF000000;
                // choose whichever pixel has the higher alpha value
                int resultRGB = pixel1Alpha > pixel2Alpha ? pixel1 : pixel2;
                // override the decision if the first alpha is full
                if (pixel1Alpha == 0xFF000000) resultRGB = pixel1;
                else if (pixel2Alpha == 0xFF000000) resultRGB = pixel2;
                // assign the chosen pixel to the result image
                result.setRGB(x, y, resultRGB);
            }
        }
        // done
        return result;
    }

}