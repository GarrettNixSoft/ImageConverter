import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

public class ImageConverter {

    public static void main(String[] args) {
        
        String usageStr = "Usage: [-help] [-m] [-merge] [-s] [-split] [-t] [-tile] [<args>]";

        if (args.length < 1) {
            System.out.println(usageStr);
            System.exit(0);
        }

        else if (args[0].contains("-help")) {
            System.out.println(usageStr + "\n");
            System.out.println("Merge: [-m] or [-merge] -- merge two images");
            System.out.println("Args: <image 1> <image 2> <mode>");
            System.out.println("Modes:");
            System.out.println("\t0: keep brightest");
            System.out.println("\n");
            System.out.println("Split: [-s] or [-split] -- split an image into subimages");
            System.out.println("Args: <image> <slice width> <slice height>");
            System.out.println("\n");
            System.out.println("Tile: [-t] or [-tile] -- combine a set of images into a tilemap");
            System.out.println("Modes:");
            System.out.println("\t[--d] or [--directory] <out image>: use all images in a directory");
            System.out.println("\t<image 1> <image 2> ... <out image> : use all image files listed");
            System.exit(0);
        }

        String command = args[0].toLowerCase().trim();

        if (command.equals("-m") || command.equals("-merge")) {
            mergeImages(args);
        }
        else if (command.equals("-s") || command.equals("-split")) {
            splitImage(args);
        }
        else if (command.equals("-t") || command.equals("-tile")) {
            tileImages(args);
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

    private static void tileImages(String[] args) {

        // example args 1: -t -d folder/subfolder out.png
        // example args 2: -t img1.png img2.png out.png

        // validate args (output file must not exist)
        String outputTarget = args[args.length - 1];
        File outputFile = Paths.get(outputTarget).toFile();
        if (outputFile.exists()) {
            System.err.println("ERROR: Target file " + outputFile.getName() + " ");
            System.exit(-1);
        }

        // get list of image files
        File[] filesToLoad;

        // Supported image formats
        String exts = ".png|.jpg";

        if (args[1].equals("--d") || args[1].equals("--directory")) {
            
            // get the target directory
            String dirPath = args[2];
            File directory = Paths.get(dirPath).toFile();

            // handle errors getting the directory
            if (!directory.exists()) {
                System.err.println("ERROR: Target directory " + directory.getName() + " does not exist!");
                System.exit(-1);
            }
            else if (!directory.isDirectory()) {
                System.err.println("ERROR: Target file " + directory.getName() + " is not a directory!");
                System.exit(-1);
            }

            // retrieve all files
            filesToLoad = directory.listFiles();

            // check if all the files are images -- if not, will need to rebuild the array
            boolean allSupportedImages = true;
            int count = 0;
            for (File file : filesToLoad) {
                
                String name = file.getName();
                String ext = name.substring(name.lastIndexOf('.'));

                if (!exts.contains(ext)) {
                    allSupportedImages = false;
                }
                else count++;

            }
            
            // if not all files are supported image files, handle it
            if (!allSupportedImages) {

                // if no files in the directory are supported image files, warn the user and exit
                if (count == 0) {
                    System.err.println("ERROR: No supported image files were found in the target directory.");
                    System.exit(-1);
                }

                // if some, but not all, files are supported image files, warn the user and continue
                System.err.println("WARNING: Not all files in the target directory are supported image types. Only supported images will be included in the output.");

                // rebuild the array
                File[] rebuilt = new File[count];

                int index = 0;
                for (File file : filesToLoad) {

                    String name = file.getName();
                    String ext = name.substring(name.lastIndexOf('.'));

                    if (exts.contains(ext)) {
                        rebuilt[index++] = file;
                    }

                }        

            }

        }
        else {

            // load files from the args list
            filesToLoad = new File[args.length - 2]; // ignore the command and the output file

            // verify all files
            for (int i = 0; i < filesToLoad.length; i++) {
                
                File file = Paths.get(args[i + 1]).toFile();
                String name = file.getName();

                // each file must exist
                if (!file.exists()) {
                    System.err.println("ERROR: File " + name + " does not exist!");
                    System.exit(-1);
                }

                // each file must be a supported image type
                String ext = name.substring(name.lastIndexOf('.'));

                if (!exts.contains(ext)) {
                    System.err.println("ERROR: File " + name + " is not a supported image file.");
                    System.exit(-1);
                }

                // if none of those failed, add the file to the list
                filesToLoad[i] = file;

            }

        }

        // load all images, track the largest dimensions
        BufferedImage[] images = new BufferedImage[filesToLoad.length];
        int maxWidth = 0;
        int maxHeight = 0;

        for (int i = 0; i < images.length; i++) {

            try {
                BufferedImage image = ImageIO.read(filesToLoad[i]);
                images[i] = image;
                if (image.getWidth() > maxWidth) maxWidth = image.getWidth();
                if (image.getHeight() > maxHeight) maxHeight = image.getHeight();
            }
            catch (IOException e) {
                System.err.println("ERROR: Could not load image file "  + filesToLoad[i].getName() + " -- " + e.getMessage());
                System.exit(-1);
            }

        }

        // determine which dimension is larger to form the tile squares
        int square = Math.max(maxWidth, maxHeight);

        // compute the smallest perfect square that fits all images
        int numImages = images.length;
        int numRows;

        // 1. if it's already a perfect square, no change is necessary
        int sqrt = (int) Math.sqrt(numImages);
        if ((int) Math.pow(sqrt, 2) == numImages) numRows = sqrt;

        // if it's not, find the next largest perfect square
        else numRows = (int) Math.ceil(Math.sqrt(numImages));

        // Announce computed size
        System.out.println("Tiling images -- grid size: " + numRows + ", cell size: " + square);

        // merge all images into a tilemap
        BufferedImage result = new BufferedImage(square * numRows, square * numRows, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numRows; c++) {

                // make sure not to overrun the image array size
                int imageIndex = r * numRows + c;
                if (imageIndex >= images.length) break;

                // fetch the image and its dimensions
                BufferedImage image = images[r * numRows + c];
                int width = image.getWidth();
                int height = image.getHeight();

                // find the center of this square
                int centerX = c * square + square / 2;
                int centerY = r * square + square / 2;

                // center the image in the square
                int x = centerX - width / 2;
                int y = centerY - height / 2;

                // draw the image at the calculated position
                g.drawImage(image, null, x, y);

            }
        }

        // save the result
        try {
            ImageIO.write(result, "png", outputFile);
        }
        catch (IOException e) {
            System.err.println("ERROR: Could not save result -- " + e.getMessage());
            System.exit(-1);
        }

    }

}