package sheepdog.one;

import sheepdog.sim.Point;

public class Player extends sheepdog.sim.Player {
    private static final double DISTANCE_BUFFER = 1.0; //1.5
	private int nblacks;
    private boolean mode;
    private int move_num;
    private double distanceInTree[];
    boolean isLeaf[];
    public  Point [] sheeps,dogs;
    Tree myTree=new Tree(this);
    static double DOG_SPEED = 1.9;
    static Point gate= new Point(50,50);
    static Point oddFinalPoint = new Point(50,40);
    static Point evenFinalPoint = new Point(50,60);
    double epsilon=1e-6;
    boolean idle;
    boolean removingOne = false;
    
    public void init(int nblacks, boolean mode) {
        this.nblacks = nblacks;
        this.mode = mode;
        move_num=0;
        idle=false;
    }
    
    // Return: the next position
    // my position: dogs[id-1]
    boolean initial=true;
    public Point move(Point[] dogs, // positions of dogs
                      Point[] sheeps) { // positions of the sheeps
    	
    	if(dogs[id-1].x<50.0 && initial)
        {            
            dogs[id-1]=initial_dog_move(dogs[id-1]);
            return dogs[id-1];
        }
    	initial=false;

    	sheeps=updateToNewSheep(sheeps,dogs);
    	Point[] whiteOnLeft=Util.findWhiteSheepOnOtherSide(sheeps, nblacks);
    	sheeps=Util.removeSheepOnLeft(sheeps, mode, nblacks);
    
    	// If Advanced mode and sheep on left
        if(mode && whiteOnLeft.length > 0 && sheeps.length==0){
            Point sheep = whiteOnLeft[0];

            // Allocate one white sheep to first dog (Nearest)
            if(id == 1 && whiteOnLeft.length>=0) {
                sheep = whiteOnLeft[0];
                System.out.println("Dog " + id + " Delivering Sheep.. " + whiteOnLeft[0]);
            }

            // Position other idle dogs away from the gate
            else if (id > 1 && whiteOnLeft.length>=0) {
                Point dogPoint;
                if (id%2 == 0) {
                    dogPoint = evenFinalPoint;
                }
                else {
                    dogPoint = oddFinalPoint;
                }

                Point dogPos = normalizeByDogSpeed(dogPoint,dogs[id-1]);
                return dogPos;
            }

            // Make dog go to gate before herding white sheep back
            if(dogs[id-1].x >= 50+epsilon)
            {            
                System.out.println("inside dog move... ");

                dogs[id-1]=initial_dog_move(dogs[id-1]);
                return dogs[id-1];
            }

            // Position dog near Sheep and herd it back to gate
            else {
                System.out.println("DEFAULT move... ");
                Point dogPoint = positionDogNearLeftSheep(sheep, gate, 1.9);

                Point from = dogPoint;
                Point to = dogs[id-1];
                double slope = (from.y - to.y)/(from.x - to.x);
                double initialDistance = distance(from, to);
                double BUFFER = 1.99;

                // Final Coordinates
                Point temp = new Point();
                temp.x = ((from.x - to.x)*BUFFER/(initialDistance)) + to.x;
                temp.y = ((from.y - to.y)*BUFFER/(initialDistance)) + to.y;

                dogPoint = temp;

                return dogPoint;
            }
        }    

        sheeps = Util.sortByAngle(sheeps);
        System.out.println("Here4");


        if (sheeps.length == 0)
            return dogs[id-1];

  /*      if(mode && sheeps.length<dogs.length){

        Point newdogs[] = new Point[sheeps.length];

        for (int i=0;i<sheeps.length;i++) {

            newdogs[i]=dogs[i];
            
        }

        dogs=newdogs;

    }*/
            
        sheeps=Util.currentDogTargets(sheeps, id, dogs.length);

        //System.out.println("Number of sheeps assigned" + sheeps.length);
    	
        move_num++;
        this.sheeps=sheeps;
        this.dogs=dogs;
        int [] father = null;
        if (move_num==1){
        	isLeaf=new boolean[sheeps.length];
        }
        distanceInTree=new double[sheeps.length];
		father=myTree.buildTree(sheeps);
		for (int i = 0; i < isLeaf.length; i++) {
			isLeaf[i]=true;
		}
		findDistanceInTree(father, -1 , 0.0);
		
		//print_array(distanceInTree);

		int[] farthest=commonSenseAllocate(1);
		
		int sheepid=farthest[0];
		
		
		
		if(sheepid!=-1 && father[sheepid]!=-1 && distance(sheeps[sheepid], sheeps[father[sheepid]])<1){
			sheepid=father[sheepid];
			//System.out.println("worked!!!");
		}
		
		
		
		Point fromPoint= sheeps[sheepid];
		Point toPoint=null;
		if(father[sheepid]==-1){
			toPoint=gate;
		}
        else 
            toPoint= sheeps[father[sheepid]];
		
		Point dogPoint = positionDogNearSheep(fromPoint, toPoint, 1.99);
		//Point dogPoint = positionDogNearSheep(toPoint, fromPoint, 0.5);
		
		/*System.out.println("from "+fromPoint.x+" "+fromPoint.y);
		System.out.println("to "+toPoint.x+" "+toPoint.y);
		System.out.println("dog point "+dogPoint.x+" "+dogPoint.y);*/
		
		Point motion=new Point(dogPoint.x-dogs[id-1].x, dogPoint.y-dogs[id-1].y);
		double motionDist=vectorLengthPoint(motion);
		if (motionDist>DOG_SPEED) {
			motion.x=motion.x/motionDist*(DOG_SPEED-epsilon);
		    motion.y=motion.y/motionDist*(DOG_SPEED-epsilon);
		}
		//System.out.println("dist of motion is "+vectorLengthPoint(motion));
		Point dogPos=new Point(dogs[id-1].x+motion.x, dogs[id-1].y+motion.y);
		//System.out.println("dog to sheep dist "+distance(dogPos, sheeps[0]));
		return dogPos;   
    }

    private Point normalizeByDogSpeed(Point dogPoint, Point point) {
    	Point motion=new Point(dogPoint.x-point.x, dogPoint.y-point.y);
		double motionDist=vectorLengthPoint(motion);
		if (motionDist>DOG_SPEED) {
			motion.x=motion.x/motionDist*(DOG_SPEED-epsilon);
		    motion.y=motion.y/motionDist*(DOG_SPEED-epsilon);
		}
		//System.out.println("dist of motion is "+vectorLengthPoint(motion));
		Point dogPos=new Point(point.x+motion.x, point.y+motion.y);// TODO Auto-generated method stub
		return dogPos;
	}

	private Point[] updateToNewSheep(Point[] sheeps,Point[] dogs) {
		Sheepdog sheepdog=new Sheepdog(dogs.length, sheeps.length, nblacks, false);
		sheepdog.sheeps=sheeps;
		sheepdog.dogs=dogs;
		
		Point[] newSheeps = new Point[sheeps.length];
        for (int i = 0; i < sheeps.length; ++i) {
            // compute its velocity vector
            newSheeps[i] = sheepdog.moveSheep(i);
        }
		return newSheeps;
	}

	private int[] commonSenseAllocate(int k) {
		int ret[]=new int[k];
		
		for (int id = 1; id <= k; id++) {
			double[] d=new double[distanceInTree.length];
	    	for (int j = 0; j < d.length; j++) {
				d[j]=distanceInTree[j]-Math.sqrt(distance(dogs[id-1], sheeps[j]))*4;//distance(dogs[id-1], sheeps[j])*DISTANCE_BUFFER;
			}
	    	for (int j = 0; j < d.length; j++) {
				if (isLeaf[j]=false)
					d[j]-=1000;
			}
	    	for (int j = 1; j < id; j++) {
				d[ret[j]]-=1000;
			}
	    	int[] index=new int[distanceInTree.length];
	    	for (int i = 0; i < index.length; i++) {
				index[i]=i;
			}
			for (int i = 0; i < distanceInTree.length; i++) {
				for (int j = i+1; j < distanceInTree.length; j++) {
					if (d[i]<d[j]) {
						double t=d[i];
						d[i]=d[j];
						d[j]=t;
						int tt=index[i];
						index[i]=index[j];
						index[j]=tt;
					}
				}
			}
			ret[id-1]=index[0];
		}
		
		return ret;
	}

	public void print_array(double[] array){
        for (int i=0; i<array.length;i++ ) {
            System.out.print(array[i] + ",  ");            
        }
        System.out.println();
    }


    public void print_array(int[] array){
        for (int i=0; i<array.length;i++ ) {
            System.out.print(array[i] + ",  ");            
        }
        System.out.println();
    }

    static Point initial_dog_move(Point dog){
            System.out.println("INside dog move");

            double dist_from_gate=distance(gate, dog);
            double move_distance=dist_from_gate;

            double speed=1.99;

            if(dist_from_gate<=speed)
                move_distance=dist_from_gate;
            else
                move_distance=speed;

            double x_new = dog.x + (move_distance)*(gate.x-dog.x)/dist_from_gate;
            double y_new = dog.y + (move_distance)*(gate.y-dog.y)/dist_from_gate;
            double temp_distance=distance(new Point(x_new, y_new), dog );

            dog.x=x_new;
            dog.y=y_new;

            return dog;            
    }
    
    // compute Euclidean distance between two points
    static double distance(Point a, Point b) {
        return Math.sqrt((a.x-b.x) * (a.x-b.x) +
                         (a.y-b.y) * (a.y-b.y));
    }
    // return the length of a vector
    static double vectorLength(double ox, double oy) {

        return Math.sqrt(ox * ox + oy * oy);
    }

    static double vectorLengthPoint(Point p) {

        return Math.sqrt(p.x*p.x + p.y*p.y);
    }
    
    //vipul
    static Point positionDogNearSheep(Point from, Point to, double BUFFER){
        // Calculate Slope, Intercept and Initial Distance
        double slope = (from.y - to.y)/(from.x - to.x);
        //System.out.println("Slope: " + slope);
        double initialDistance = distance(from, to);
        //System.out.println("Initial Dist: " + initialDistance);

        // Final Coordinates
        Point finalDestination = new Point();
        finalDestination.x = ((from.x - to.x)*(initialDistance + BUFFER)/(initialDistance)) + to.x;
        finalDestination.y = ((from.y - to.y)*(initialDistance + BUFFER)/(initialDistance)) + to.y;

        // Error Checking
        if (finalDestination.x > 100)
        {
		finalDestination.x = 100;
		finalDestination.y = slope*(100 - to.x) + to.y;
        }

        if (finalDestination.x < 50)
        {
	       finalDestination.x = 50;
	       finalDestination.y = slope*(50 - to.x) + to.y;
        }

        if (finalDestination.y > 100){
            finalDestination.y = 100;
            finalDestination.x = (100 - to.y)/slope + to.x;
        }

        if (finalDestination.y < 0){
            finalDestination.y = 0;
            finalDestination.x = (0 - to.y)/slope + to.x;
        }

        //System.out.println("X: " + finalDestination.x);
        //System.out.println("Y: " + finalDestination.y);

        return finalDestination;
    }

    static Point positionDogNearLeftSheep(Point from, Point to, double BUFFER) {
        // Calculate Slope, Intercept and Initial Distance
        double slope = (from.y - to.y)/(from.x - to.x);
        //System.out.println("Slope: " + slope);
        double initialDistance = distance(from, to);
        //System.out.println("Initial Dist: " + initialDistance);

        System.out.println("HERE POSITIONING DOG NEAR LEFT SHEEP");

        // Final Coordinates
        Point finalDestination = new Point();
        finalDestination.x = ((from.x - to.x)*(initialDistance + BUFFER)/(initialDistance)) + to.x;
        finalDestination.y = ((from.y - to.y)*(initialDistance + BUFFER)/(initialDistance)) + to.y;

        // Error Checking
        // if (finalDestination.x > 100)
        // {
        // finalDestination.x = 100;
        // finalDestination.y = slope*(100 - to.x) + to.y;
        // }

        // if (finalDestination.x > 50)
        // {
        //    finalDestination.x = 50;
        //    finalDestination.y = slope*(50 - to.x) + to.y;
        // }

        // if (finalDestination.y > 100){
        //     finalDestination.y = 100;
        //     finalDestination.x = (100 - to.y)/slope + to.x;
        // }

        // if (finalDestination.y < 0){
        //     finalDestination.y = 0;
        //     finalDestination.x = (0 - to.y)/slope + to.x;
        // }

        return finalDestination;
    }

    private void findDistanceInTree(int[] father, int index, double distance){
        double temp_distance = 0.0;

        //System.out.println("Print index: " + index + "  " + "Print distance:" + distance);

        for (int i=0;i<sheeps.length;i++) {

            if (father[i] == index) {

                if(index == -1)
                    temp_distance = distance(gate, sheeps[i]);
                else{
                    temp_distance = distance(sheeps[index], sheeps[i]);
                    isLeaf[index]=false;
                }

                distanceInTree[i] = distance+temp_distance;

                findDistanceInTree(father, i, distance+temp_distance);
                
            }  
            
        }

         return;

    }
}