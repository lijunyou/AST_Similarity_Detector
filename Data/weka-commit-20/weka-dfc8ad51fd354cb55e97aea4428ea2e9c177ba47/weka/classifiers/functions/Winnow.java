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
 *    Winnow.java
 *    Copyright (C) 2002 J. Lindgren
 *
 */
package weka.classifiers.functions;

import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.Filter;
import weka.classifiers.*;
import weka.core.*;
import java.util.*;

/**
 *
 * Implements Winnow and Balanced Winnow algorithms by
 * N. Littlestone. For more information, see<p>
 *
 * N. Littlestone (1988). <i> Learning quickly when irrelevant
 * attributes are abound: A new linear threshold algorithm</i>.
 * Machine Learning 2, pp. 285-318.<p>
 *
 * and
 * 
 * N. Littlestone (1989). <i> Mistake bounds and logarithmic 
 * linear-threshold learning algorithms</i>. Technical report
 * UCSC-CRL-89-11, University of California, Santa Cruz.<p>
 *
 * Valid options are:<p>
 *
 * -L <br>
 * Use the baLanced variant (default: false)<p>
 *
 * -I num <br>
 * The number of iterations to be performed. (default 1)<p>
 *
 * -A double <br>
 * Promotion coefficient alpha. (default 2.0)<p>
 *
 * -B double <br>
 * Demotion coefficient beta. (default 0.5)<p>
 *
 * -W double <br>
 * Starting weights of the prediction coeffs. (default 2.0)<p>
 *
 * -H double <br>
 * Prediction threshold. (default -1.0 == number of attributes)<p>
 *
 * -S int <br>
 * Random seed to shuffle the input. (default 1), -1 == no shuffling<p>
 *
 * @author J. Lindgren (jtlindgr<at>cs.helsinki.fi)
 * @version $Revision: 1.7 $ 
*/
public class Winnow extends Classifier implements UpdateableClassifier {
  
  /** Use the balanced variant? **/
  protected boolean m_Balanced;
 
  /** The number of iterations **/
  protected int m_numIterations = 1;

  /** The promotion coefficient **/
  protected double m_Alpha = 2.0;

  /** The demotion coefficient **/
  protected double m_Beta = 0.5;

  /** Prediction threshold, <0 == numAttributes **/
  protected double m_Threshold = -1.0;
  
  /** Random seed used for shuffling the dataset, -1 == disable **/
  protected int m_Seed = 1;

  /** Accumulated mistake count (for statistics) **/
  protected int m_Mistakes;

  /** Starting weights for the prediction vector(s) **/
  protected double m_defaultWeight = 2.0;
  
  /** The weight vectors for prediction **/
  private double[] m_predPosVector = null;
  private double[] m_predNegVector = null;

  /** The true threshold used for prediction **/
  private double m_actualThreshold;

  /** The training instances */
  private Instances m_Train = null;

  /** The filter used to make attributes numeric. */
  private NominalToBinary m_NominalToBinary;

  /** The filter used to get rid of missing values. */
  private ReplaceMissingValues m_ReplaceMissingValues;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Implements Winnow and Balanced Winnow algorithms by "
      + "Littlestone. For more information, see\n\n"
      + "N. Littlestone (1988). \"Learning quickly when irrelevant "
      + "attributes are abound: A new linear threshold algorithm\". "
      + "Machine Learning 2, pp. 285-318.\n\n"
      + "and\n\n"
      + "N. Littlestone (1989). \"Mistake bounds and logarithmic  "
      + "linear-threshold learning algorithms\". Technical report "
      + "UCSC-CRL-89-11, University of California, Santa Cruz.\n\n"
      + "Does classification for problems with nominal attributes "
      + "(which it converts into binary attributes).";
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(7);
    
    newVector.addElement(new Option("\tUse the baLanced version\n"
				    + "\t(default false)",
				    "L", 0, "-L"));
    newVector.addElement(new Option("\tThe number of iterations to be performed.\n"
				    + "\t(default 1)",
				    "I", 1, "-I <int>"));
    newVector.addElement(new Option("\tPromotion coefficient alpha.\n"
				    + "\t(default 2.0)",
				    "A", 1, "-A <double>"));
    newVector.addElement(new Option("\tDemotion coefficient beta.\n"
				    + "\t(default 0.5)",
				    "B", 1, "-B <double>"));
    newVector.addElement(new Option("\tPrediction threshold.\n"
				    + "\t(default -1.0 == number of attributes)",
				    "H", 1, "-H <double>"));
    newVector.addElement(new Option("\tStarting weights.\n"
				    + "\t(default 2.0)",
				    "W", 1, "-W <double>"));
    newVector.addElement(new Option("\tDefault random seed.\n"
				    + "\t(default 1)",
				    "S", 1, "-S <int>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    m_Balanced = Utils.getFlag('L', options);
	
    String iterationsString = Utils.getOption('I', options);
    if (iterationsString.length() != 0) {
      m_numIterations = Integer.parseInt(iterationsString);
    }
    String alphaString = Utils.getOption('A', options);
    if (alphaString.length() != 0) { 
      m_Alpha = (new Double(alphaString)).doubleValue();
    }
    String betaString = Utils.getOption('B', options);
    if (betaString.length() != 0) {
      m_Beta = (new Double(betaString)).doubleValue();
    }
    String tString = Utils.getOption('H', options);
    if (tString.length() != 0) {
      m_Threshold = (new Double(tString)).doubleValue();
    }
    String wString = Utils.getOption('W', options);
    if (wString.length() != 0) {
      m_defaultWeight = (new Double(wString)).doubleValue();
    }
    String rString = Utils.getOption('S', options);
    if (rString.length() != 0) {
      m_Seed = Integer.parseInt(rString);
    }
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {

    String[] options = new String [20];
    int current = 0;

    if(m_Balanced) {
      options[current++] = "-L"; 
    }
    
    options[current++] = "-I"; options[current++] = "" + m_numIterations;
    options[current++] = "-A"; options[current++] = "" + m_Alpha;
    options[current++] = "-B"; options[current++] = "" + m_Beta;
    options[current++] = "-H"; options[current++] = "" + m_Threshold;
    options[current++] = "-W"; options[current++] = "" + m_defaultWeight;
    options[current++] = "-S"; options[current++] = "" + m_Seed;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Builds the classifier
   *
   * @exception Exception if something goes wrong during building
   */
  public void buildClassifier(Instances insts) throws Exception {

    if (insts.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Can't handle string attributes!");
    }
    if (insts.numClasses() > 2) {
      throw new Exception("Can only handle two-class datasets!");
    }
    if (insts.classAttribute().isNumeric()) {
      throw new UnsupportedClassTypeException("Can't handle a numeric class!");
    }
    Enumeration enum = insts.enumerateAttributes();
    while (enum.hasMoreElements()) {
      Attribute attr = (Attribute) enum.nextElement();
      if (!attr.isNominal()) {
        throw new UnsupportedAttributeTypeException("Winnow: only nominal attributes, "
						    + "please.");
      }
    }

    // Filter data
    m_Train = new Instances(insts);
    m_Train.deleteWithMissingClass();
    
    m_ReplaceMissingValues = new ReplaceMissingValues();
    m_ReplaceMissingValues.setInputFormat(m_Train);
    m_Train = Filter.useFilter(m_Train, m_ReplaceMissingValues);
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(m_Train);
    m_Train = Filter.useFilter(m_Train, m_NominalToBinary);

    /** Randomize training data */
    if(m_Seed != -1) {
      m_Train.randomize(new Random(m_Seed));
    }

    /** Make space to store weights */
    m_predPosVector = new double[m_Train.numAttributes()];

    if(m_Balanced) {
      m_predNegVector = new double[m_Train.numAttributes()];
    }

    /** Initialize the weights to starting values **/
    for(int i = 0; i < m_Train.numAttributes(); i++)
      m_predPosVector[i] = m_defaultWeight;

    if(m_Balanced) {
      for(int i = 0; i < m_Train.numAttributes(); i++) {
	m_predNegVector[i] = m_defaultWeight;
      }
    }
	
    /** Set actual prediction threshold **/
    if(m_Threshold<0) {
      m_actualThreshold = (double)m_Train.numAttributes()-1;
    } else {
      m_actualThreshold = m_Threshold;
    }

    m_Mistakes=0;

    /** Compute the weight vectors **/
    if(m_Balanced) {
      for (int it = 0; it < m_numIterations; it++) {
	for (int i = 0; i < m_Train.numInstances(); i++) {
	  actualUpdateClassifierBalanced(m_Train.instance(i));
	}
      }
    } else {
      for (int it = 0; it < m_numIterations; it++) {
	for (int i = 0; i < m_Train.numInstances(); i++) {
	  actualUpdateClassifier(m_Train.instance(i));
	}
      }
    }
  }
  
  /**
   * Updates the classifier with a new learning example
   *
   * @exception Exception if something goes wrong
   */
  public void updateClassifier(Instance instance) throws Exception {
	
    m_ReplaceMissingValues.input(instance);
    m_ReplaceMissingValues.batchFinished();
    Instance filtered = m_ReplaceMissingValues.output();
    m_NominalToBinary.input(filtered);
    m_NominalToBinary.batchFinished();
    filtered = m_NominalToBinary.output();

    if(m_Balanced) {
      actualUpdateClassifierBalanced(filtered);
    } else {
      actualUpdateClassifier(filtered);
    }
  }
  
  /**
   * Actual update routine for prefiltered instances
   *
   * @exception Exception if something goes wrong
   */
  private void actualUpdateClassifier(Instance inst) throws Exception {
    
    double posmultiplier;
	
    if (!inst.classIsMissing()) {
      double prediction = makePrediction(inst);
   
      if (prediction != inst.classValue()) {
	m_Mistakes++;

	if(prediction == 0) {
	  /* false neg: promote */
	  posmultiplier=m_Alpha;
	} else {
	  /* false pos: demote */
	  posmultiplier=m_Beta;
	}
	int n1 = inst.numValues(); int classIndex = m_Train.classIndex();
	for(int l = 0 ; l < n1 ; l++) {
	  if(inst.index(l) != classIndex && inst.valueSparse(l)==1) {
	    m_predPosVector[inst.index(l)]*=posmultiplier;
	  }
	}
	//Utils.normalize(m_predPosVector);
      }
    }
    else {
      System.out.println("CLASS MISSING");
    }
  }
  
  /**
   * Actual update routine (balanced) for prefiltered instances
   *
   * @exception Exception if something goes wrong
   */
  private void actualUpdateClassifierBalanced(Instance inst) throws Exception {
    
    double posmultiplier,negmultiplier;

    if (!inst.classIsMissing()) {
      double prediction = makePredictionBalanced(inst);
        
      if (prediction != inst.classValue()) {
	m_Mistakes++;
	
	if(prediction == 0) {
	  /* false neg: promote positive, demote negative*/
	  posmultiplier=m_Alpha;
	  negmultiplier=m_Beta;
	} else {
	  /* false pos: demote positive, promote negative */
	  posmultiplier=m_Beta;
	  negmultiplier=m_Alpha;
	}
	int n1 = inst.numValues(); int classIndex = m_Train.classIndex();
	for(int l = 0 ; l < n1 ; l++) {
	  if(inst.index(l) != classIndex && inst.valueSparse(l)==1) {
	    m_predPosVector[inst.index(l)]*=posmultiplier;
	    m_predNegVector[inst.index(l)]*=negmultiplier;
	  }
	}
	//Utils.normalize(m_predPosVector);
	//Utils.normalize(m_predNegVector);
      }
    }
    else {
      System.out.println("CLASS MISSING");
    }
  }

  /**
   * Outputs the prediction for the given instance.
   *
   * @param inst the instance for which prediction is to be computed
   * @return the prediction
   * @exception Exception if something goes wrong
   */
  public double classifyInstance(Instance inst) throws Exception {

    m_ReplaceMissingValues.input(inst);
    m_ReplaceMissingValues.batchFinished();
    Instance filtered = m_ReplaceMissingValues.output();
    m_NominalToBinary.input(filtered);
    m_NominalToBinary.batchFinished();
    filtered = m_NominalToBinary.output();

    if(m_Balanced) {
      return(makePredictionBalanced(filtered));
    } else {
      return(makePrediction(filtered));
    }
  }
  
  /** 
   * Compute the actual prediction for prefiltered instance
   *
   * @param inst the instance for which prediction is to be computed
   * @return the prediction
   * @exception Exception if something goes wrong
   */
  private double makePrediction(Instance inst) throws Exception {

    double total = 0;

    int n1 = inst.numValues(); int classIndex = m_Train.classIndex();
	
    for(int i=0;i<n1;i++) {
      if(inst.index(i) != classIndex && inst.valueSparse(i)==1) {
	total+=m_predPosVector[inst.index(i)];
      }
    }
    
    if(total > m_actualThreshold) {
      return(1);
    } else {
      return(0);
    }
  }
  
  /** 
   * Compute our prediction (Balanced) for prefiltered instance 
   *
   * @param inst the instance for which prediction is to be computed
   * @return the prediction
   * @exception Exception if something goes wrong
   */
  private double makePredictionBalanced(Instance inst) throws Exception {
    double total=0;
	
    int n1 = inst.numValues(); int classIndex = m_Train.classIndex();
    for(int i=0;i<n1;i++) {
      if(inst.index(i) != classIndex && inst.valueSparse(i)==1) {
	total+=(m_predPosVector[inst.index(i)]-m_predNegVector[inst.index(i)]);
      }
    }
     
    if(total > m_actualThreshold) {
      return(1);
    } else {
      return(0);
    }
  }

  /**
   * Returns textual description of the classifier.
   */
  public String toString() {

    if(m_predPosVector==null)
      return("Winnow: No model built yet.");
	   
    String result = "Winnow\n\nAttribute weights\n\n";
	
    int classIndex = m_Train.classIndex();

    if(!m_Balanced) {
      for( int i = 0 ; i < m_Train.numAttributes(); i++) {
	if(i!=classIndex)
	  result += "w" + i + " " + m_predPosVector[i] + "\n";
      }
    } else {
      for( int i = 0 ; i < m_Train.numAttributes(); i++) {
	if(i!=classIndex) {
	  result += "w" + i + " p " + m_predPosVector[i];
	  result += " n " + m_predNegVector[i];
	  
	  double wdiff=m_predPosVector[i]-m_predNegVector[i];
	  
	  result += " d " + wdiff + "\n";
	}
      }
    }
    result += "\nCumulated mistake count: " + m_Mistakes + "\n\n";
	
    return(result);
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String balancedTipText() {
    return "Whether to use the balanced version of the algorithm.";
  }

  /**
   * Get the value of Balanced.
   *
   * @return Value of Balanced.
   */
  public boolean getBalanced() {
    
    return m_Balanced;
  }
  
  /**
   * Set the value of Balanced.
   *
   * @param b  Value to assign to Balanced.
   */
  public void setBalanced(boolean b) {
    
    m_Balanced = b;
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String alphaTipText() {
    return "Promotion coefficient alpha.";
  }
  
  /**
   * Get the value of Alpha.
   *
   * @return Value of Alpha.
   */
  public double getAlpha() {
    
    return(m_Alpha);
  }
  
  /**
   * Set the value of Alpha.
   *
   * @param a  Value to assign to Alpha.
   */
  public void setAlpha(double a) {
    
    m_Alpha = a;
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String betaTipText() {
    return "Demotion coefficient beta.";
  }
  
  /**
   * Get the value of Beta.
   *
   * @return Value of Beta.
   */
  public double getBeta() {
    
    return(m_Beta);
  }
  
  /**
   * Set the value of Beta.
   *
   * @param b  Value to assign to Beta.
   */
  public void setBeta(double b) {
    
    m_Beta = b;
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String thresholdTipText() {
    return "Prediction threshold (-1 means: set to number of attributes).";
  }
  
  /**
   * Get the value of Threshold.
   *
   * @return Value of Threshold.
   */
  public double getThreshold() {
    
    return m_Threshold;
  }
  
  /**
   * Set the value of Threshold.
   *
   * @param t  Value to assign to Threshold.
   */
  public void setThreshold(double t) {
    
    m_Threshold = t;
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String defaultWeightTipText() {
    return "Initial value of weights/coefficients.";
  }
  
  /**
   * Get the value of defaultWeight.
   *
   * @return Value of defaultWeight.
   */
  public double getDefaultWeight() {
    
    return m_defaultWeight;
  }
  
  /**
   * Set the value of defaultWeight.
   *
   * @param w  Value to assign to defaultWeight.
   */
  public void setDefaultWeight(double w) {
    
    m_defaultWeight = w;
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numIterationsTipText() {
    return "The number of iterations to be performed.";
  }
  
  /**
   * Get the value of numIterations.
   *
   * @return Value of numIterations.
   */
  public int getNumIterations() {
    
    return m_numIterations;
  }
  
  /**
   * Set the value of numIterations.
   *
   * @param v  Value to assign to numIterations.
   */
  public void setNumIterations(int v) {
    
    m_numIterations = v;
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "Random number seed used for data shuffling (-1 means no "
      + "randomization).";
  }

  /**
   * Get the value of Seed.
   *
   * @return Value of Seed.
   */
  public int getSeed() {
    
    return m_Seed;
  }
  
  /**
   * Set the value of Seed.
   *
   * @param v  Value to assign to Seed.
   */
  public void setSeed(int v) {
    
    m_Seed = v;
  }
  
  /**
   * Main method.
   */
  public static void main(String[] argv) {
    
    try {
      System.out.println(Evaluation.evaluateModel(new Winnow(), argv));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
