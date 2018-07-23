package org.schemaspy.model;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.StatUtils;


import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by LASFE using IntelliJ on 7/20/2014.
 */
public class Scorer {
    //Test set comes from http://www.jiaaro.com/KNN-for-humans/
    /*
          red         1
          orange      2
          yellow      3
          green       4
          blue        5
          purple      6

    */

    //The results of the program can be found at http://www.jiaaro.com/KNN-for-humans/
    public double[][] input =  {
            {371, 3, 6},
            {378, 3, 4},
            //{355, 3, 4},
            //{362, 3, 2},
            //{379, 3, 4}
    };

    //public String[] label;
    public double[] predict =  {371, 3, 6}; //mutations hash to be processed
    public HashMap clusters = new HashMap();
    HashMap<Double, Integer> euclideanDistances = new HashMap();

    public int k = 6; //Number of clusters
    public int max_iterations = 1000;

    //Turn out RealMatrix into a hash with each key being set to each row
    private static HashMap matrixToHash(RealMatrix mat){
        HashMap hash = new HashMap();

        for(int i=0; i<mat.getRowDimension(); i++){
            hash.put(i, mat.getRow(i));
        }

        return hash;
    }

    public double score (GenericTreeNode gtn,GenericTree mutationTree)
    {
        flushContext();
        initContext(gtn,mutationTree);
        Scorer sc = new Scorer();
        //Lets create the centroids or 'average' locations of center for our points
        double[][] centroids;

        //Lets standardize our input array
        sc.input = MatrixUtils.createRealMatrix(sc.input).getData();

        //Lets put an array in each of the clusters to append the each {weight, color, # of seeds} to
        for(int i=0; i<sc.k; i++)
            sc.clusters.put(i, new double[sc.input[0].length]);

        sc.solve();

        //Now lets predict our test array
        //sc.closestClusterIndex(euclideanDistances); // not used right now cause we need ALL the distances.

        return computeScore();
    }


    /*private void predictClass(double[][] centroids){

        int index = euclideanDistance( this.predict,  centroids );
        System.out.println(Arrays.toString(this.predict) + " is closest to Centroid " + index);
    }*/

    public int closestClusterIndex(HashMap<Double,Integer> map)
    {
        SortedSet<Double> keys = new TreeSet<Double>(map.keySet());
        return map.get( keys.first());
    }


    private void solve(){
        //Let create two random sets of centroids to compare for convergence later
        double [][] centroids = createRandomCentroids(this.k, this.input);
        double [][] oldCentroids = createRandomCentroids(this.k,  this.input);

        int iterations = 0;

        //We need a dynamic array to store our points
        HashMap<Integer, ArrayList<double[]>> clusters = new HashMap<Integer, ArrayList<double[]>>();

        //Lets run the algorithm until it converges or reaches max iterations
        while( this.converged(oldCentroids, centroids, iterations) != true ){

            oldCentroids = centroids;

            clusters = this.findClosestCentroids( this.input, centroids);
            centroids = this.getNewCentroids( clusters);

            //System.out.println( Arrays.deepToString(this.clusters.values().toArray()) );
            iterations += 1;
        }

    }

    //Lets assign 'labels' or 'outputs' to each of our 'clusters' or grouped set of points
   /* private void assignLabels( HashMap<Integer, ArrayList<double[]>> clusters ){
        //Lets turn out list of outputs into a unique set
        Set mySet = new HashSet(Arrays.asList(this.output));
        this.label = new String[clusters.size()];

        //Lets take the first point in each cluster, see the its index in the input and use
        //that index to get the label from the output
        for(int i=0; i< clusters.size(); i++){
            int index =  ArrayUtils.indexOf(this.input , clusters.get(i).get(0) );

            this.label[i] = this.output[index];
        }

        //System.out.println(Arrays.deepToString( this.label ) );
    }*/

    //Calculates the mean of the new centroids via the clusters in each group
    private double[][] getNewCentroids( HashMap<Integer, ArrayList<double[]>> hash){
        double[][] newCentroids = new double[hash.size()][];

        for(Map.Entry entry: hash.entrySet())
        {
            ArrayList tmp = new ArrayList();
            tmp = (ArrayList) entry.getValue();
            System.out.println(tmp);
            if(tmp.isEmpty())
            {
                double[] dummy = {0,0,0};
                ((ArrayList) entry.getValue()).add(dummy);
            }
        }

        System.out.println("hash"+hash);

        for(int i=0;i<hash.size();i++){
            //Lets create a matrix of each groups points to index them by column easier
            RealMatrix mat = MatrixUtils.createRealMatrix(hash.get(i).toArray(new double[][]{}));
            double[] mean = new double[mat.getColumnDimension()];
            //Now lets iterate through each column(weight, color, type) and set that value to the mean
            //of our centroid
            for (int j = 0; j < mat.getColumnDimension(); j++) {
                mean[j] = StatUtils.mean(mat.getColumn(j));

            }

            newCentroids[i] = mean;//Setting the centroids new mean
        }

        return newCentroids;
    }

    private boolean converged(double [][] oldCentroids, double [][] centroids, int iterations){
        //Dont want to iterate forever.  Break of the algorithm at 'max_iterations'
        if(iterations>this.max_iterations ) {
            System.out.println("Max iterations reached. Returning...");
            return true;
        }
        //If my old and new centroids are equal after comparing which centroid each data point was equal
        //to then we have converged
        if( Arrays.deepEquals(oldCentroids, centroids) ) {
            System.out.println("Centroids have converged. Returning...");
            return true;
        }

        return false;
    }

    //Creating Random Centroids with values that are in the range of our data points
    private double[][] createRandomCentroids(int row,  double[][] input){

        RealMatrix mat =  MatrixUtils.createRealMatrix(input) ;
        int column = input[0].length;
        //Lets create k centroids that have the same number of indices as our inputs
        double[][] centroids = new double[row][column];
        Random rand = new Random();

        for(int i=0;i<row;i++) {
            for (int j = 0; j < mat.getColumnDimension(); j++) {
                //Lets get the max and min of each columns
                double max = mat.getColumnVector(j).getMaxValue(),
                        min = mat.getColumnVector(j).getMinValue();

                //Now lets create a random point in between the max and min values of the column
                centroids[i][j] =min + (max - min) * rand.nextDouble();

            }
        }


        return  centroids;
    }

    //We need to find the centroids that have the shortest Euclidean distance to each input
    private HashMap findClosestCentroids(double[][] input, double [][] centroids){

        HashMap<Integer, ArrayList> clusters = new HashMap();
        for(int i=0;i<centroids.length;i++)//Lets prepopulate our hash with Arraylists to add arrays
            clusters.put(i, new ArrayList<double[]>() );

        for(double[] arr: input){

            //Index of centroid with shorted distance to this input
            int index = closestClusterIndex(euclideanDistance( arr,  centroids ));

            //Now lets add the input to the centroids cluster grouping
            clusters.get(index).add(arr);

        }
        return clusters;
    }

    //Perform Euclidean distance formula to find out the distance
    //between our prediction value and each row in the matrix
    public HashMap<Double,Integer> euclideanDistance( double[] input, double[][] centroids ){

        RealMatrix m = MatrixUtils.createRealMatrix( centroids );

        //Lets turn out 'y' value or label into vector for easier math operations
        RealVector Y = MatrixUtils.createRealVector( input);

        for (int i=0; i<m.getRowDimension(); i++){
            RealVector vec = m.getRowVector(i);


            RealVector sub = vec.subtract( Y );

            //Take square root of sum of square values that were subtracted a line above
            double distance = Math.sqrt(StatUtils.sumSq(sub.toArray()));
            //Use the distance to each data point(or row) as key with the 'default' option as value
            euclideanDistances.put( distance  , i/*cluster number*/ );
        }
        System.out.println(euclideanDistances);
        //Now lets sort the map's keys into a set
        SortedSet<Double> keys = new TreeSet<Double>(euclideanDistances.keySet());
        List<Integer> neighbors = new ArrayList<Integer>();

        return euclideanDistances;//Return cluster index of shortest distance
    }

    private double computeScore()
    {
        double res = 0;
        for(Double distance : euclideanDistances.keySet())
        {
            res = res + distance;
        }
        return res;
    }

    private void initContext(GenericTreeNode gtn,GenericTree mutationTree)
    {
        int i = 0;
        ReportVector rpv = gtn.getReportVector();
        rpv.setStackTraceHash(rpv.hashStackTrace(mutationTree,gtn));
        predict = rpv.getStackTraceHash();
        for(GenericTreeNode gtnLoop : mutationTree.toArray())
        {
            double[] data = gtnLoop.getReportVector().getStackTraceHash();
            input[i] = data;
            i++;
        }
    }

    private void flushContext()
    {
        predict = null;
        input = null;
        clusters = new HashMap();
    }

}

