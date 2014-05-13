package org.waterapps.lib;

/**
 * Created by Steve on 7/21/13.
 */


/**
 * An interface to read DEM data from files
 */
public interface ReadDemData {

    /**
     * Reads a DemData object from a specified file
     * @param filename File location to be read
     * @return DemData object read from file
     */
    public DemData readFromFile(String filename);
}
