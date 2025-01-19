package jazarifresnelsim.ui;

// IRenderer.java

/**
 * Interface for rendering the Fresnel solar collector simulation.
 * Implements the Strategy pattern for different rendering approaches.
 */
public interface IRenderer {
    /**
     * Renders a single frame of the simulation
     */
    void render();
    
   
    /**
     * Handles camera setup and movement
     */
    void setupCamera();
    
    /**
     * Clean up resources
     */
    void dispose();
}
