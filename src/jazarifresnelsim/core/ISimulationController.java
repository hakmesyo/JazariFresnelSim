package jazarifresnelsim.core;

import controlP5.ControlP5;
import java.time.LocalDateTime;

/**
 * Interface for controlling the Fresnel solar collector simulation.
 * Defines the contract for simulation control operations.
 */
public interface ISimulationController {
    /**
     * Starts the simulation
     */
    void startSimulation();
    
    /**
     * Stops the simulation
     */
    void stopSimulation();
    
    /**
     * Updates the simulation state for the current time step
     */
    void update();
    
    /**
     * Sets the simulation time range
     * @param startTime Start time of simulation
     * @param endTime End time of simulation
     */
    void setTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Sets the simulation step size in minutes
     * @param stepMinutes Time step in minutes
     */
    void setSimulationStep(double stepMinutes);
    
    /**
     * Sets the location parameters
     * @param latitude Latitude in degrees
     * @param longitude Longitude in degrees
     */
    void setLocation(double latitude, double longitude);
    void updateSolarPosition();
    void updateMirrorPositions();
    void updateGUIDisplay(ControlP5 cp5);
}
