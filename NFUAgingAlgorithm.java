import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class NFUAgingAlgorithm {
	private static HashMap pgTable = new HashMap<>();	//Page Table
	private static int oldestAgeValue;
	private static int oldestPage;
	private static int msbForAge;
	private static List<Integer> pageReferences;
	
	//Update ages of all page entries in the pade table
	public static void updateAges() {
		
		for(Object kv: pgTable.entrySet())
		{
			Map.Entry kvPair = (Map.Entry)kv;
			int ageVal = (int) kvPair.getValue();
			
	        ageVal = ageVal >> 1;
			pgTable.replace(kvPair.getKey(),ageVal);
			
			if(ageVal<oldestAgeValue){ //keep track of oldest page entry in the page table
				oldestPage = (int) kvPair.getKey();
			}
			oldestAgeValue = ageVal;	
		}
	}
	
	//This funciton counts nuber of page faults for the given frame size
	public static int computePageFaultCount(int frameSize) {
		int currFrameCount = 0;
		int i=0;
		int faultCount = 0;
		pgTable = new HashMap<>();
		
		while(currFrameCount<frameSize && i<pageReferences.size()) {
			
			//Get the current page reference
			int entry = pageReferences.get(i);
			
			//since a page is referenced, update the ages of all the pages in the page table
			updateAges(); 
			
			if(pgTable.containsKey(entry)){		//If page is available in the page table
				int ageValue = (int) pgTable.get(entry);
				ageValue = ageValue | msbForAge;	//set MSB value for the age of page i.
				pgTable.replace(entry, ageValue);	//Set the updated value in page table
			} 
			else //If the page entry is not present in the age table. 
			{ 
				faultCount = faultCount + 1; //fault has occurred
				
				if(pgTable.size()<frameSize) // if there is room in the page table
				{
					pgTable.put(entry, msbForAge); //add the page entry with msb set for age.
				}
				else if(pgTable.size()==frameSize) //if page table is full.
				{ 
					pgTable.remove(oldestPage);	// Remove the oldest page entry from the table
					pgTable.put(entry,msbForAge); //put the current entry into the table and set its age MSB
				}
			}
			
			currFrameCount++;
			if(currFrameCount>=frameSize){	//Repeat for every frame size
				currFrameCount = 0;
			}
			
			i++;
		}
		return faultCount;
	}
	
	public static void main(String args[]) {
		
		int frameSizeArr[] = {4,8,16,32,64,128,256,512,1024}; //Various frame sizes.
		float[] faultPer1000 = new float[frameSizeArr.length]; //Array to record rate of page fault/1000
		
		msbForAge = 16777216; //This is to set the MSB 24bit age reference bit- 2^24
		oldestAgeValue = msbForAge; //This will track the oldest age value
		pageReferences = new ArrayList<Integer>();//This would contain page references from the PageReferences.txt file.
		
		//Reading page entries from PageReferences.txt
		BufferedReader buff = null;
		try 
		{
			String ref;
			buff = new BufferedReader(new FileReader("PageReferences.txt"));
			while ((ref = buff.readLine()) != null) {
				pageReferences.add(Integer.parseInt(ref));
			}

		} catch (IOException e) {
			e.printStackTrace();
		} 
		finally 
		{		
			try {
				if (buff != null) buff.close();
			} 
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		for(int k=0; k < frameSizeArr.length; k++) {
			int pageFaultCount=computePageFaultCount(frameSizeArr[k]);
			faultPer1000[k] = (float)((float) 1000/pageReferences.size())*pageFaultCount;
			System.out.println("Frame size :" + frameSizeArr[k]+"\t\t\t"
							 + "Total Fault Count :" + pageFaultCount + "\t\t\t"
							 + "Page fault/1000 :"+faultPer1000[k]);
			System.out.println("_______________________________________________________________________________________________");
			
		}
		
		//Plot graph for Frame size vs Page Fault/1000
		Graph graph = new Graph(frameSizeArr, faultPer1000);
	    graph.pack( );
	    RefineryUtilities.centerFrameOnScreen( graph );
	    graph.setVisible( true );

	}
}

class Graph extends ApplicationFrame
{
   int[] frames;
   float[] faultper1000;
   public Graph(int[] frames, float[] faultper1000)
   {
      super("Total Frames vs Page Fault/1000");
      
      DefaultCategoryDataset data = new DefaultCategoryDataset( );
      for(int i=0; i<frames.length;i++)
      {
    	  data.addValue( faultper1000[i] , "Page Fault/1000" ,  "" + frames[i]);
      }
      //Plotting the graph
      JFreeChart lC = ChartFactory.createLineChart(
    		  												"Total Frames vs Page Fault/1000",
    		  												"Total Frames","Page Fault/1000",
    		  												data,
    		  												PlotOrientation.VERTICAL,
    		  												true,
    		  												true,
    		  												false);
         
      ChartPanel cp = new ChartPanel(lC);
      cp.setPreferredSize(new java.awt.Dimension(600,400));
      setContentPane(cp);
   }
}
