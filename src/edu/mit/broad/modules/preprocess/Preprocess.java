package edu.mit.broad.modules.preprocess;
import org.genepattern.io.*;
import org.genepattern.io.expr.*;
import org.genepattern.data.matrix.*;
import org.genepattern.data.expr.*;
import org.genepattern.module.*;
import java.util.Arrays;
import java.util.Random;
import gnu.trove.*;
/**
 *  Performs a variety of pre-processing operations including
 *  thresholding/ceiling,variation filter, row normalization and log2 transform
 *
 *@author     jgould
 *@created    March 17, 2004
 */
public class Preprocess {

    static double log2=Math.log(2);
	static void printUsage() {
		System.err.println("java Preprocess -F <input.file> -O <output file name>");
		System.err.println("The following options are available:");
		System.err.println("-r <output file format>");
		System.err.println("-V <filter> 0 = no filter (default), 1 = filter");
		System.err.println("-n <row normalization> 0 = no normalization (default), 1 = normalization");
		System.err.println("-f <minimum fold change for filter>");
		System.err.println("-d <minimum delta for filter>");
		System.err.println("-t <value for thresholding>");
		System.err.println("-c <value for ceiling>");
		System.err.println("-s <maximum sigma for binning>");
		System.err.println("-p <value for uniform probability threshold filter>");
		System.err.println("-e <number of experiments to exclude (max & min) before applying var. filter>");
        System.err.println("-a <min. number of columns> Remove row if < min number of cols is above threshold");
        System.err.println("-b <threshold for removing rows>");
		System.err.println("-g <log2 transform> 0 = no transform (default), 1 = log2 transform");
	}


	/**
	 *  The main program for the Preprocess class
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) {
		String inputFile = null;

		final int GCT_OUTPUT_FORMAT = 0;
		final int RES_OUTPUT_FORMAT = 1;
		final int ODF_OUTPUT_FORMAT = 2;
		final int SAME_OUTPUT_FORMAT = 3;
		int outputFormat = SAME_OUTPUT_FORMAT;

		int filterFlag = 0; // 0=no filter (default), 1=filter
		int rownormFlag = 0; // 0=no disc or norm (default), 1=disc., 2=normalization
		int log2Flag = 0;  // 0=no transform, 1=log2 transform
		double minFoldChange = 1;
		double minDelta = 0;

		double threshold = 20;
		double ceiling = 16000;
		double remColThreshold = 0;
		int columnsAboveThreshold = -1;

		double probabilityThreshold = 1; // value for uniform probablity threshold filter
		int numExclude = 0; // number of experiments to exclude (max & min before applying variation filter
		String outputFileName = null;

		for(int i = 0, length = args.length; i < length; i++) {
			if(args[i].charAt(0) != '-') {
				AnalysisUtil.exit("options must start with '-'");
			}
			switch (args[i].charAt(1)) {
				case 'O':
					outputFileName = args[++i];
					break;
				case 'f':
					minFoldChange = Double.parseDouble(args[++i]);
					break;
				case 'd':
					minDelta = Double.parseDouble(args[++i]);
					break;
				case 't':
					threshold = Double.parseDouble(args[++i]);
					break;
				case 'c':
					ceiling = Double.parseDouble(args[++i]);
					break;
				case 'p':
					probabilityThreshold = Double.parseDouble(args[++i]);
					break;
				case 'e':
					numExclude = Integer.parseInt(args[++i]);
					break;
				case 'n':
					rownormFlag = Integer.parseInt(args[++i]);
					if(rownormFlag != 0 && rownormFlag != 1 && rownormFlag != 2) {
						AnalysisUtil.exit("Illegal option for row normalization.");
					}
					break;
				case 'F':
					inputFile = args[++i];
					break;
				case 'r':
					outputFormat = Integer.parseInt(args[++i]);
					if(outputFormat != GCT_OUTPUT_FORMAT && outputFormat != RES_OUTPUT_FORMAT && outputFormat != ODF_OUTPUT_FORMAT && outputFormat != SAME_OUTPUT_FORMAT) {
						AnalysisUtil.exit("Illegal option for output format.");
					}

					break;
				case 'g':
					log2Flag = Integer.parseInt(args[++i]);
					if(log2Flag != 0 && log2Flag != 1) {
						AnalysisUtil.exit("Illegal option for log2 transform.");
					}
					break;
                case 'a':
                    try {
                        columnsAboveThreshold = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException nfe) {
                        AnalysisUtil.exit("Number of columns above threshold is not a number");
                    }
                    if (columnsAboveThreshold <= 0) {
                        AnalysisUtil.exit("Number of columns above threshold must be > 0");
                    }
                case 'b':
                    try {
                        remColThreshold = Double.parseDouble(args[++i]);
                    } catch (NumberFormatException nfe) {
                        System.err.println("threshold seen: " + args[i] + ".");
                       AnalysisUtil.exit("Minimum threshold is not a number");

                    }
				case 'V':
					filterFlag = Integer.parseInt(args[++i]);
					if(filterFlag != 0 && filterFlag != 1) {
						AnalysisUtil.exit("Illegal option for filtering.");
					}
					break;
				case 'R': // for backwards compatibility
					break;
				default:
					AnalysisUtil.exit("unrecognized option: " + args[i]);
			}
		}

        if(rownormFlag == 1 && log2Flag == 1) {
            AnalysisUtil.exit("Cannot take the log2 of zero-centered data due to negative values. Please select row normalization OR log2 transform.");
        }

		if(inputFile == null) {
			AnalysisUtil.exit("No input file specified.");
		}

		if(outputFileName == null) {
			AnalysisUtil.exit("No output file specified.");
		}

		IExpressionDataReader reader = AnalysisUtil.getExpressionReader(inputFile);
		ExpressionData expressionData = (ExpressionData) AnalysisUtil.readExpressionData(reader, inputFile, new PreprocessExpressionDataCreator());
		DoubleMatrix2D dataset = (DoubleMatrix2D) expressionData.getExpressionMatrix();
		double[] geneArray = new double[dataset.getColumnCount()];
		TIntArrayList rowIndices = new TIntArrayList(); // indices to keep
		int columns = dataset.getColumnCount();
		Random random = new Random();
		for(int i = 0, rows = dataset.getRowCount(); i < rows; i++) {
			double mean = 0;
			double temp = 0;
			int numColAboveThres = 0;
			for(int j = 0; j < columns; j++) {
				double datum = dataset.get(i, j);
                if (columnsAboveThreshold != -1 && datum >= remColThreshold) {
                    numColAboveThres++;
                }
				if(filterFlag == 1) {
					if(datum < threshold) {
						dataset.set(i, j, threshold);
					}
					if(datum > ceiling) {
						dataset.set(i, j, ceiling);
					}
				}
				geneArray[j] = dataset.get(i, j);
				mean = mean + geneArray[j];
				temp = temp + geneArray[j] * geneArray[j];
			}

			Arrays.sort(geneArray);
			double min = geneArray[numExclude];
			if(min == 0) {
				min = 0.001;
			}
			double max = geneArray[columns - 1 - numExclude];
			mean = mean / columns;
            double sigma=temp / columns;
            sigma=sigma - mean * mean;
            sigma=(sigma>0) ? Math.sqrt(sigma) : 0.001;

			if( ( (filterFlag == 0 && random.nextDouble() < probabilityThreshold) ||
			    (filterFlag == 1 && Math.abs(max)/Math.abs(min) >= minFoldChange && max - min >= minDelta))
			    && (columnsAboveThreshold == -1 || numColAboveThres >= columnsAboveThreshold) ) {
				rowIndices.add(i);
                for(int k = 0; k < columns; k++) {
                    if(rownormFlag == 1) {
                        dataset.set(i, k, (dataset.get(i, k) - mean) / sigma);
                    }

                    if(log2Flag == 1) {
                       double x=Math.log(dataset.get(i, k));
                       x/=log2;
                       dataset.set(i, k, x);
                    }
                }
			}
		}

		int[] indices = rowIndices.toNativeArray();
		if (indices.length == 0) {
			AnalysisUtil.exit("Filtered all genes.");
		}

		String outputFormatName = null;
		switch (outputFormat) {
			case GCT_OUTPUT_FORMAT:
				outputFormatName = "gct";
				break;
			case RES_OUTPUT_FORMAT:
				outputFormatName = "res";
				break;
			case ODF_OUTPUT_FORMAT:
				outputFormatName = "odf";
				break;
			case SAME_OUTPUT_FORMAT:
				outputFormatName = reader.getFormatName();
				break;
		}

		AnalysisUtil.write(expressionData.slice(rowIndices.toNativeArray(), null), outputFormatName, outputFileName, true);
	}
}
