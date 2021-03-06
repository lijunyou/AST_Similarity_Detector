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
 *    PairedCorrectedTTester.java
 *    Copyright (C) 2003 Richard Kirkby
 *
 */


package weka.experiment;

import weka.core.*;

/**
 * Behaves the same as PairedTTester, only it uses the corrected
 * resampled t-test statistic.<p>
 *
 * For more information see:<p>
 *
 * Claude Nadeau and Yoshua Bengio, "Inference for the Generalization Error,"
 * Machine Learning, 2001.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class PairedCorrectedTTester extends PairedTTester {

  /**
   * Computes a paired t-test comparison for a specified dataset between
   * two resultsets.
   *
   * @param datasetSpecifier the dataset specifier
   * @param resultset1Index the index of the first resultset
   * @param resultset2Index the index of the second resultset
   * @param comparisonColumn the column containing values to compare
   * @return the results of the paired comparison
   * @exception Exception if an error occurs
   */
  public PairedStats calculateStatistics(Instance datasetSpecifier,
					 int resultset1Index,
					 int resultset2Index,
					 int comparisonColumn) throws Exception {

    if (m_Instances.attribute(comparisonColumn).type()
	!= Attribute.NUMERIC) {
      throw new Exception("Comparison column " + (comparisonColumn + 1)
			  + " ("
			  + m_Instances.attribute(comparisonColumn).name()
			  + ") is not numeric");
    }
    if (!m_ResultsetsValid) {
      prepareData();
    }

    Resultset resultset1 = (Resultset) m_Resultsets.elementAt(resultset1Index);
    Resultset resultset2 = (Resultset) m_Resultsets.elementAt(resultset2Index);
    FastVector dataset1 = resultset1.dataset(datasetSpecifier);
    FastVector dataset2 = resultset2.dataset(datasetSpecifier);
    String datasetName = templateString(datasetSpecifier);
    if (dataset1 == null) {
      throw new Exception("No results for dataset=" + datasetName
			 + " for resultset=" + resultset1.templateString());
    } else if (dataset2 == null) {
      throw new Exception("No results for dataset=" + datasetName
			 + " for resultset=" + resultset2.templateString());
    } else if (dataset1.size() != dataset2.size()) {
      throw new Exception("Results for dataset=" + datasetName
			  + " differ in size for resultset="
			  + resultset1.templateString()
			  + " and resultset="
			  + resultset2.templateString()
			  );
    }

    // calculate the test/train ratio
    double testTrainRatio = 0.0;
    int trainSizeIndex = -1;
    int testSizeIndex = -1;
    // find the columns with the train/test sizes
    for (int i=0; i<m_Instances.numAttributes(); i++) {
      if (m_Instances.attribute(i).name().equals("Number_of_training_instances")) {
	trainSizeIndex = i;
      } else if (m_Instances.attribute(i).name().equals("Number_of_testing_instances")) {
	testSizeIndex = i;
      }
    }
    if (trainSizeIndex >= 0 && testSizeIndex >= 0) {
      double totalTrainSize = 0.0;
      double totalTestSize = 0.0;
      for (int k = 0; k < dataset1.size(); k ++) {
	Instance current = (Instance) dataset1.elementAt(k);
	totalTrainSize += current.value(trainSizeIndex);
	totalTestSize += current.value(testSizeIndex);
      }
      testTrainRatio = totalTestSize / totalTrainSize;
    }
    PairedStats pairedStats =
      new PairedStatsCorrected(m_SignificanceLevel, testTrainRatio);

    for (int k = 0; k < dataset1.size(); k ++) {
      Instance current1 = (Instance) dataset1.elementAt(k);
      Instance current2 = (Instance) dataset2.elementAt(k);
      if (current1.isMissing(comparisonColumn)) {
	throw new Exception("Instance has missing value in comparison "
			    + "column!\n" + current1);
      }
      if (current2.isMissing(comparisonColumn)) {
	throw new Exception("Instance has missing value in comparison "
			    + "column!\n" + current2);
      }
      if (current1.value(m_RunColumn) != current2.value(m_RunColumn)) {
	System.err.println("Run numbers do not match!\n"
			    + current1 + current2);
      }
      double value1 = current1.value(comparisonColumn);
      double value2 = current2.value(comparisonColumn);
      pairedStats.add(value1, value2);
    }
    pairedStats.calculateDerived();
    return pairedStats;
  }  
}
