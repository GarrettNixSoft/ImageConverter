import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

public class ImageConverter {

    public static void main(String[] args) {
        
        if (args.length < 1) {
            System.out.println("Usage: [-help] [-m] [-merge] [-s] [-split] [<args>]");
            System.exit(0);
        }

        else if (args[0].contains("-help")) {
            System.out.println("Usage: [-help] [-m] [-merge] [-s] [-split] [<args>]\n");
            System.out.println("Merge: [-m] or [-merge] -- merge two images");
            System.out.println("Args: <image 1> <image 2> <mode>");
            System.out.println("Modes:");
            System.out.println("\t0: keep brightest");
            System.out.println("\n");
            System.out.println("Split: [-s] or [-split]");
            System.out.println("Args: <image> <slice width> <slice height>");
            System.exit(0);
        }

        String command = args[0].toLowerCase().trim();

        if (command.equals("-m") || command.equals("-merge")) {
            mergeImages(args);
        }
        else if (command.equals("-s") || command.equals("-split")) {
            splitImage(args);
        }

    }

    private static void mergeImages(String[] args) {
        
        String path1Str = args[1];
        String path2Str = args[2];
        String destStr = args[3];
        String modeStr = args[4];

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

    private static void splitImage(String[] args) {
        
        // organize the args
        String imagePath = args[1];
        String widthStr = args[2];
        String heightStr = args[3];

        // fetch the image file
        Path path = Paths.get(imagePath);
        File file = path.toFile();

        if (!file.exists()) {
            System.err.println("Error: " + file.getName() + " does not exist.");
            System.out.println("(Full path: " + file.getAbsolutePath() + ")");
            System.exit(-1);
        }

        // parse the integers
        int width = 0;
        int height = 0;
        try {
            width = Integer.parseInt(widthStr);
            height = Integer.parseInt(heightStr);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing dimensions: please enter valid integers only.");
            System.exit(-1);
        }

        // load the image to split
        BufferedImage image = null;

        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
            System.exit(-1);
        }

        // check that the dimensions are correct
        if (image.getWidth() % width != 0) {
            System.err.println("Error: split width does not evenly divide image width (" + image.getWidth() + " / " + width + ")");
            System.exit(-1);
        }
        else if (image.getHeight() % height != 0) {
            System.err.println("Error: split height does not evenly divide image height (" + image.getHeight() + " / " + height + ")");
            System.exit(-1);
        }

        // prepare an array of subimages
        int rows = image.getHeight() / height;
        int cols = image.getWidth() / width;
        BufferedImage[][] subimages = new BufferedImage[rows][cols];

        System.out.println("Splitting into " + rows + " rows and " + cols + " columns.");

        // split the image
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c * width;
                int y = r * height;
                subimages[r][c] = image.getSubimage(x, y, width, height);
            }
        }

        // determine the name pattern for the result files
        String subimageBaseName = file.getAbsolutePath();
        subimageBaseName = subimageBaseName.substring(0, subimageBaseName.lastIndexOf('.'));

        // save all subimages to files
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String subimageFileName = subimageBaseName + "_" + r + "_" + c + ".png";
                File dest = new File(subimageFileName);
                try {
                    ImageIO.write(subimages[r][c], "png", dest);
                } catch (IOException e) {
                    System.err.println("Error saving subimage: " + e.getMessage());
                }
            }
        }

        // done
        System.out.println("Success!");

    }
}