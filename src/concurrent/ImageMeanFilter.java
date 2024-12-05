import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class provides functionality to apply a mean filter to an image.
 * The mean filter is used to smooth images by averaging the pixel values
 * in a neighborhood defined by a kernel size.
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * ImageMeanFilter.applyMeanFilter("input.jpg", "output.jpg", 3);
 * }
 * </pre>
 * 
 * <p>Supported image formats: JPG, PNG</p>
 * 
 * <p>Author: temmanuel@comptuacao.ufcg.edu.br</p>
 */
public class ImageMeanFilter {
    
    /**
     * Applies mean filter to an image
     * 
     * @param inputPath  Path to input image
     * @param outputPath Path to output image 
     * @param kernelSize Size of mean kernel
     * @throws IOException If there is an error reading/writing
     */
    public static void applyMeanFilter(String inputPath, String outputPath, int kernelSize, int threadNumber) throws IOException {
        // Load image
        BufferedImage originalImage = ImageIO.read(new File(inputPath));
        
        // Create result image
        BufferedImage filteredImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            BufferedImage.TYPE_INT_RGB
        );

        int rowByThread = (originalImage.getHeight() / threadNumber);
        Thread[] threads = new Thread[threadNumber];

        for(int i=0; i < threadNumber; i++){
            int firstRow = i * rowByThread;
            int lastRow;
            if(i == threadNumber-1){
                lastRow = originalImage.getHeight();
            }else{
                lastRow = rowByThread * (i+1);
            }
            Task task = new Task(originalImage, filteredImage, kernelSize, firstRow, lastRow);
            Thread thread = new Thread(task);
            threads[i] = thread;
            thread.start();
        }

        for(Thread thread: threads){
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
      
        // Save filtered image
        ImageIO.write(filteredImage, "jpg", new File(outputPath));
        
    }

    public static class Task implements Runnable{
        private final BufferedImage originalImage;
        private final BufferedImage filteredImage;
        private final int kernelSize;
        private final int firstRow;
        private final int lastRow;

        public Task(BufferedImage originalImage, BufferedImage filteredImage, int kernelSize, int firstRow, int lastRow){
            this.originalImage = originalImage;
            this.filteredImage = filteredImage;
            this.kernelSize = kernelSize;
            this.firstRow = firstRow;
            this.lastRow = lastRow;
        }

        @Override
        public void run(){
            int width = this.originalImage.getWidth();
            for (int y = firstRow; y < lastRow; y++) {
                for (int x = 0; x < width; x++) {
                    int[] avgColor = calculateNeighborhoodAverage(originalImage, x, y, kernelSize);
                    filteredImage.setRGB(x, y, 
                        (avgColor[0] << 16) | 
                        (avgColor[1] << 8)  | 
                        avgColor[2]
                    );
                }
            }

        }

         /**
     * Calculates average colors in a pixel's neighborhood
     * 
     * @param image      Source image
     * @param centerX    X coordinate of center pixel
     * @param centerY    Y coordinate of center pixel
     * @param kernelSize Kernel size
     * @return Array with R, G, B averages
     */
        private static int[] calculateNeighborhoodAverage(BufferedImage image, int centerX, int centerY, int kernelSize) {
            int width = image.getWidth();
            int height = image.getHeight();
            int pad = kernelSize / 2;
            
            // Arrays for color sums
            long redSum = 0, greenSum = 0, blueSum = 0;
            int pixelCount = 0;
            
            // Process neighborhood
            for (int dy = -pad; dy <= pad; dy++) {
                for (int dx = -pad; dx <= pad; dx++) {
                    int x = centerX + dx;
                    int y = centerY + dy;
                    
                    // Check image bounds
                    if (x >= 0 && x < width && y >= 0 && y < height) {
                        // Get pixel color
                        int rgb = image.getRGB(x, y);
                        
                        // Extract color components
                        int red = (rgb >> 16) & 0xFF;
                        int green = (rgb >> 8) & 0xFF;
                        int blue = rgb & 0xFF;
                        
                        // Sum colors
                        redSum += red;
                        greenSum += green;
                        blueSum += blue;
                        pixelCount++;
                    }
                }
            }
        
        // Calculate average
            return new int[] {
                (int)(redSum / pixelCount),
                (int)(greenSum / pixelCount),
                (int)(blueSum / pixelCount)
            };
        
        }

    }
    
    
    /**
     * Main method for demonstration
     * 
     * Usage: java ImageMeanFilter <input_file>
     * 
     * Arguments:
     *   input_file - Path to the input image file to be processed
     *                Supported formats: JPG, PNG
     * 
     * Example:
     *   java ImageMeanFilter input.jpg
     * 
     * The program will generate a filtered output image named "filtered_output.jpg"
     * using a 7x7 mean filter kernel
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ImageMeanFilter <input_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        int threadNumber = Integer.parseInt(args[1]);

        try {
            applyMeanFilter(inputFile, "filtered_output.jpg", 7, threadNumber);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        }
    }
}
