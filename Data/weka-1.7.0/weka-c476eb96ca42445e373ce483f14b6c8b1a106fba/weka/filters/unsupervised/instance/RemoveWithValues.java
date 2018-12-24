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
 *    RemoveWithValues.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */


package weka.filters.unsupervised.instance;

import weka.filters.*;
import java.util.Enumeration;
import java.util.Vector;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.core.UnsupportedAttributeTypeException;

/** 
 * Filters instances according to the value of an attribute.<p>
 *
 * Valid filter-specific options are:<p>
 *
 * -C num<br>
 * Choose attribute to be used for selection (default last).<p>
 *
 * -S num<br>
 * Numeric value to be used for selection on numeric attribute.
 * Instances with values smaller than given value will be selected.
 * (default 0) <p>
 *
 * -L index1,index2-index4,...<br>
 * Range of label indices to be used for selection on nominal attribute.
 * First and last are valid indexes. (default all values)<p>
 *
 * -M <br>
 * Missing values count as a match. This setting is independent of
 * the -V option. (default missing values don't match)<p>
 *
 * -V<br>
 * Invert matching sense.<p>
 *
 * -H<br>
 * When selecting on nominal attributes, removes header references to
 * excluded values. <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class RemoveWithValues extends Filter
  implements UnsupervisedFilter, StreamableFilter, OptionHandler {

  /** Stores the attribute setting */
  protected int m_AttributeSet = -1;

  /** Stores which attribute to be used for filtering */
  protected int m_Attribute;
  
  /** Stores which values of nominal attribute are to be used for filtering.*/
  protected Range m_Values;

  /** Stores which value of a numeric attribute is to be used for filtering.*/
  protected double m_Value = 0;

  /** Inverse of test to be used? */
  protected boolean m_Inverse = false;

  /** True if missing values should count as a match */
  protected boolean m_MatchMissingValues = false;

  /** Modify header for nominal attributes? */
  protected boolean m_ModifyHeader = false;

  /** If m_ModifyHeader, stores a mapping from old to new indexes */
  protected int [] m_NominalMapping;

  /** Default constructor */
  public RemoveWithValues() {

      m_Values = new Range("first-last");
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(5);

    newVector.addElement(new Option(
              "\tChoose attribute to be used for selection.",
              "C", 1, "-C <num>"));
    newVector.addElement(new Option(
              "\tNumeric value to be used for selection on numeric\n"+
	      "\tattribute.\n"+
	      "\tInstances with values smaller than given value will\n"+
              "\tbe selected. (default 0)",
              "S", 1, "-S <num>"));
    newVector.addElement(new Option(
              "\tRange of label indices to be used for selection on\n"+
	      "\tnominal attribute.\n"+
	      "\tFirst and last are valid indexes. (default all values)",
              "L", 1, "-L <index1,index2-index4,...>"));
    newVector.addElement(new Option(
	      "\tMissing values count as a match. This setting is\n"+
              "\tindependent of the -V option.\n"+
              "\t(default missing values don't match)",
              "M", 0, "-M"));
    newVector.addElement(new Option(
	      "\tInvert matching sense.",
              "V", 0, "-V"));
    newVector.addElement(new Option(
	      "\tWhen selecting on nominal attributes, removes header\n"
	      + "\treferences to excluded values.",
              "H", 0, "-H"));

    return newVector.elements();
  }


  /**
   * Parses a given list of options.
   * Valid options are:<p>
   *
   * -C num<br>
   * Choose attribute to be used for selection (default last).<p>
   *
   * -S num<br>
   * Numeric value to be used for selection on numeric attribute.
   * Instances with values smaller than given value will be selected.
   * (default 0) <p>
   *
   * -L index1,index2-index4,...<br>
   * Range of label indices to be used for selection on nominal attribute.
   * First and last are valid indexes. (default all values)<p>
   *
   * -M <br>
   * Missing values count as a match. This setting is independent of
   * the -V option. (default missing values don't match)<p>
   *
   * -V<br>
   * Invert matching sense.<p>
   *
   * -H<br>
   * When selecting on nominal attributes, removes header references to
   * excluded values. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String attIndex = Utils.getOption('C', options);
    if (attIndex.length() != 0) {
      if (attIndex.toLowerCase().equals("last")) {
	setAttributeIndex(-1);
      } else if (attIndex.toLowerCase().equals("first")) {
        setAttributeIndex(0);
      } else {
	setAttributeIndex(Integer.parseInt(attIndex) - 1);
      }
    } else {
      setAttributeIndex(-1);
    }

    String splitPoint = Utils.getOption('S', options);
    if (splitPoint.length() != 0) {
      setSplitPoint((new Double(splitPoint)).doubleValue());
    } else {
      setSplitPoint(0);
    }

    String convertList = Utils.getOption('L', options);
    if (convertList.length() != 0) {
      setNominalIndices(convertList);
    } else {
      setNominalIndices("");
    }
    setInvertSelection(Utils.getFlag('V', options));
    setMatchMissingValues(Utils.getFlag('M', options));
    setModifyHeader(Utils.getFlag('H', options));
    // Re-initialize output format according to new options
    
    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [8];
    int current = 0;

    options[current++] = "-S"; options[current++] = "" + getSplitPoint();
    options[current++] = "-C";
    options[current++] = "" + (getAttributeIndex() + 1);
    if (!getNominalIndices().equals("")) {
      options[current++] = "-L"; options[current++] = getNominalIndices();
    }
    if (getInvertSelection()) {
      options[current++] = "-V";
    }
    if (getModifyHeader()) {
      options[current++] = "-H";
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @exception UnsupportedAttributeTypeException if the specified attribute
   * is neither numeric or nominal.
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);
    if (m_AttributeSet == -1) {
      m_Attribute = instanceInfo.numAttributes() - 1;
    } else {
      m_Attribute = m_AttributeSet;
    }
    if (!isNumeric() && !isNominal()) {
      throw new UnsupportedAttributeTypeException("Can only handle numeric or nominal attributes.");
    }
    m_Values.setUpper(instanceInfo.attribute(m_Attribute).numValues() - 1);
    if (isNominal() && m_ModifyHeader) {
      instanceInfo = new Instances(instanceInfo, 0); // copy before modifying
      Attribute oldAtt = instanceInfo.attribute(m_Attribute);
      int [] selection = m_Values.getSelection();
      FastVector newVals = new FastVector();
      for (int i = 0; i < selection.length; i++) {
	newVals.addElement(oldAtt.value(selection[i]));
      }
      instanceInfo.deleteAttributeAt(m_Attribute);
      instanceInfo.insertAttributeAt(new Attribute(oldAtt.name(), newVals),
				      m_Attribute);
      m_NominalMapping = new int [oldAtt.numValues()];
      for (int i = 0; i < m_NominalMapping.length; i++) {
	boolean found = false;
	for (int j = 0; j < selection.length; j++) {
	  if (selection[j] == i) {
	    m_NominalMapping[i] = j;
	    found = true;
	    break;
	  }
	}
	if (!found) {
	  m_NominalMapping[i] = -1;
	}
      }
    }
    setOutputFormat(instanceInfo);
    return true;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been set.
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (instance.isMissing(m_Attribute)) {
      if (getMatchMissingValues()) {
        push((Instance)instance.copy());
        return true;
      } else {
        return false;
      }
    }
    if (isNumeric()) {
      if (!m_Inverse) {
	if (Utils.sm(instance.value(m_Attribute), m_Value)) {
	  push((Instance)instance.copy());
	  return true;
	} 
      } else {
	if (Utils.grOrEq(instance.value(m_Attribute), m_Value)) {
	  push((Instance)instance.copy());
	  return true;
	} 
      }
    }
    if (isNominal()) {
      if (m_Values.isInRange((int)instance.value(m_Attribute))) {
	Instance temp = (Instance)instance.copy();
	if (getModifyHeader()) {
	  temp.setValue(m_Attribute,
			m_NominalMapping[(int)instance.value(m_Attribute)]);
	}
	push(temp);
	return true;
      }
    }
    return false;
  }

  /** 
   * Returns true if selection attribute is nominal.
   *
   * @return true if selection attribute is nominal
   */
  public boolean isNominal() {
    
    if (getInputFormat() == null) {
      return false;
    } else {
      return getInputFormat().attribute(m_Attribute).isNominal();
    }
  }

  /** 
   * Returns true if selection attribute is numeric.
   *
   * @return true if selection attribute is numeric
   */
  public boolean isNumeric() {
    
    if (getInputFormat() == null) {
      return false;
    } else {
      return getInputFormat().attribute(m_Attribute).isNumeric();
    }
  }
  
  /**
   * Gets whether the header will be modified when selecting on nominal
   * attributes.
   *
   * @return true if so.
   */
  public boolean getModifyHeader() {
    
    return m_ModifyHeader;
  }
  
  /**
   * Sets whether the header will be modified when selecting on nominal
   * attributes.
   *
   * @param newModifyHeader true if so.
   */
  public void setModifyHeader(boolean newModifyHeader) {
    
    m_ModifyHeader = newModifyHeader;
  }
  
  /**
   * Get the attribute to be used for selection (-1 for last)
   *
   * @return the attribute index
   */
  public int getAttributeIndex() {

    return m_AttributeSet;
  }

  /**
   * Sets attribute to be used for selection
   *
   * @param attribute the attribute's index (-1 for last);
   */
  public void setAttributeIndex(int attribute) {

    m_AttributeSet = attribute;
  }

  /**
   * Get the split point used for numeric selection
   *
   * @return the numeric split point
   */
  public double getSplitPoint() {

    return m_Value;
  }

  /**
   * Split point to be used for selection on numeric attribute.
   *
   * @param value the split point
   */
  public void setSplitPoint(double value) {

    m_Value = value;
  }

  /**
   * Gets whether missing values are counted as a match.
   *
   * @return true if missing values are counted as a match.
   */
  public boolean getMatchMissingValues() {

    return m_MatchMissingValues;
  }
  
  /**
   * Sets whether missing values are counted as a match.
   *
   * @param newMatchMissingValues true if missing values are counted as a match.
   */
  public void setMatchMissingValues(boolean newMatchMissingValues) {

    m_MatchMissingValues = newMatchMissingValues;
  }
  
  /**
   * Get whether the supplied columns are to be removed or kept
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return m_Values.getInvert();
  }

  /**
   * Set whether selected values should be removed or kept. If true the 
   * selected values are kept and unselected values are deleted. 
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_Inverse = invert;
    m_Values.setInvert(invert);
  }

  /**
   * Get the set of nominal value indices that will be used for selection
   *
   * @return rangeList a string representing the list of nominal indices.
   */
  public String getNominalIndices() {

    return m_Values.getRanges();
  }

  /**
   * Set which nominal labels are to be included in the selection.
   *
   * @param rangeList a string representing the list of nominal indices.
   * eg: first-3,5,6-last
   * @exception InvalidArgumentException if an invalid range list is supplied
   */
  public void setNominalIndices(String rangeList) {
    
    m_Values.setRanges(rangeList);
  }

  /**
   * Set which values of a nominal attribute are to be used for
   * selection.
   *
   * @param values an array containing indexes of values to be
   * used for selection
   * @exception InvalidArgumentException if an invalid set of ranges is supplied
   */
  public void setNominalIndicesArr(int [] values) {

    String rangeList = "";
    for(int i = 0; i < values.length; i++) {
      if (i == 0) {
	rangeList = "" + (values[i] + 1);
      } else {
	rangeList += "," + (values[i] + 1);
      }
    }
    setNominalIndices(rangeList);
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new RemoveWithValues(), argv);
      } else {
	Filter.filterFile(new RemoveWithValues(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








