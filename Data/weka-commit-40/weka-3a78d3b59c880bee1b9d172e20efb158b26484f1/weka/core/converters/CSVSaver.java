/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    CSVSaver.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package weka.core.converters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import weka.core.Instance;
import weka.core.Instances;

/**
 * Writes to a destination in csv format.
 *
 * Valid options:
 *
 * -i input arff file <br>
 * The input filw in arff format. <p>
 *
 * -o the output file <br>
 * The output file. The prefix of the output file is sufficient. If no output file is given, Saver tries to use standard out. <p>
 * 
 *
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 * @see Saver
 */
public class CSVSaver extends AbstractSaver implements BatchConverter, IncrementalConverter, FileSourcedConverter {

   
  /**
   * Returns a string describing this Loader
   * @return a description of the Loader suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Writes to a destination that is in csv format";
  }

  
  /**
   * Returns a description of the file type.
   *
   * @return a short file description
   */
  public String getFileDescription() {
    return "CSV file: comma separated files";
  }

  /**
   * Resets the Saver 
   */
  public void resetOptions() {

    super.resetOptions();
    setFileExtension(".csv");
  }


  /** Saves an instances incrementally. Structure has to be set by using the
   * setStructure() method or setInstances() method.
   * @param inst the instance to save
   * @throws IOException throws IOEXception if an instance cannot be saved incrementally.
   */  
  public void writeIncremental(Instance inst) throws IOException{
  
      int writeMode = getWriteMode();
      Instances structure = getInstances();
      PrintWriter outW = null;
      
      if(getRetrieval() == BATCH || getRetrieval() == NONE)
          throw new IOException("Batch and incremental saving cannot be mixed.");
      if(getWriter() != null)
          outW = new PrintWriter(getWriter());
          
      if(writeMode == WAIT){
        if(structure == null){
            setWriteMode(CANCEL);
            if(inst != null)
                System.err.println("Structure(Header Information) has to be set in advance");
        }
        else
            setWriteMode(STRUCTURE_READY);
        writeMode = getWriteMode();
      }
      if(writeMode == CANCEL){
          if(outW != null)
              outW.close();
          cancel();
      }
      if(writeMode == STRUCTURE_READY){
          setWriteMode(WRITE);
          //write header
          if(getFile() == null || outW == null){
              // print out attribute names as first row
              for (int i = 0; i < structure.numAttributes(); i++) {
                System.out.print(structure.attribute(i).name());
                if (i < structure.numAttributes()-1) {
                    System.out.print(",");
                } else {
                    System.out.println();
                }
              } 
          }
          else{
              for (int i = 0; i < structure.numAttributes(); i++) {
                outW.print(structure.attribute(i).name());
                if (i < structure.numAttributes()-1) {
                    outW.print(",");
                } else {
                    outW.println();
                }
              }
              outW.flush();
          }
          writeMode = getWriteMode();
      }
      if(writeMode == WRITE){
          if(structure == null)
              throw new IOException("No instances information available.");
          if(inst != null){
          //write instance 
              if(getFile() == null || outW == null)
                System.out.println(inst);
              else
                outW.println(inst);
          }
          else{
          //close
              if(outW != null){
                outW.flush();
                outW.close();
              }
              setWriteMode(WAIT);
              setStructure(null);
          }
      }
  }  

  /** Writes a Batch of instances
   * @throws IOException throws IOException if saving in batch mode is not possible
   */
  public void writeBatch() throws IOException {
  
      if(getInstances() == null)
          throw new IOException("No instances to save");
      if(getRetrieval() == INCREMENTAL)
          throw new IOException("Batch and incremental saving cannot be mixed.");
      setRetrieval(BATCH);
      setWriteMode(WRITE);
      if(getFile() == null || getWriter() == null){
          // print out attribute names as first row
          for (int i = 0; i < getInstances().numAttributes(); i++) {
            System.out.print(getInstances().attribute(i).name());
            if (i < getInstances().numAttributes()-1) {
                System.out.print(",");
            } else {
            System.out.println();
            }
        }
        for (int i = 0; i < getInstances().numInstances(); i++) {
            System.out.println(getInstances().instance(i));
        }
        return;
      }
      PrintWriter outW = new PrintWriter(getWriter());
      // print out attribute names as first row
      for (int i = 0; i < getInstances().numAttributes(); i++) {
	outW.print(getInstances().attribute(i).name());
	if (i < getInstances().numAttributes()-1) {
	  outW.print(",");
	} else {
	  outW.println();
	}
      }
      for (int i = 0; i < getInstances().numInstances(); i++) {
	outW.println(getInstances().instance(i));
      }
      outW.flush();
      outW.close();
      setWriteMode(WAIT);
  }

  /**
   * Main method.
   *
   * @param options should contain the options of a Saver.
   */
  public static void main(String [] options) {
      try {
	CSVSaver csv = new CSVSaver();
        try {
          csv.setOptions(options);  
        } catch (Exception ex) {
            ex.printStackTrace();
	}
        //incre,ental
        /*
        csv.setRetrieval(INCREMENTAL);
        Instances instances = csv.getInstances();
        csv.setStructure(instances);
        for(int i = 0; i <= instances.numInstances(); i++){ //last instance is null and finishes incremental saving
            csv.writeIncremental(instances.instance(i));
        }
        */
        
        //batch
        csv.writeBatch();
      } catch (Exception ex) {
	ex.printStackTrace();
	}
      
    }
}
  
  
