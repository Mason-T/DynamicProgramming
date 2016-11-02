package telescope;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

public class Main {

	static int dim;						// stores the dimension of the space we're working in. The examples from class are in 1 
	static ArrayList<Event> events;		// stores all the events from in the input file, in increasing chronological time
	static HashSet<Event> toPush;		// stores all of the events that need to be pushed down the possibility hash. Saves us from computing the same thing repeatedly
	static HashMap<Event, Event> path;  // if there is a known path to the end from an event, the next event to be observed is stored here
	static HashMap<Event, Integer> possible;  // records the current maximum number of possible events that each event can observe after itself (even if the path is not known)
	static TreeMap<Integer, HashSet<Event>> possibleInverse;  // a reverse hash of possible

	public static void main( String[] args ) throws Exception {
		writeInput( "inputrand", 10000, 100, 1 );
		readInput( "inputrand" );
		pruneEvents();
		
		System.out.println( "start" );
		long time = System.currentTimeMillis();
		Event e = findMaxObservable();
		time = System.currentTimeMillis() - time;
		printPath( e );
		System.out.println( "Time taken: " + time );
	}
	
	public static Event findMaxObservable() {

		path = new HashMap<Event, Event>();
		possible = new HashMap<Event, Integer>();
		possibleInverse = new TreeMap<Integer, HashSet<Event>>();
		possibleInverse.put( 1, new HashSet<Event>() );
		toPush = new HashSet<Event>();
		
		// Adds all the events to the possibility hash in reverse order
		for( int i = events.size() - 1; i >= 0; i-- ) {
			Event event = events.get( i );
			int currentMax = possibleInverse.lastKey();
			// Assume that the next event has the possibility to observe 1 more event than any of the others
			if( !possibleInverse.get( currentMax ).isEmpty() ) {
				possibleInverse.put( ++currentMax, new HashSet<Event>() );
			}
			updatePossibility( event, currentMax );
			// If it not, then we push it down
			if( !check( event ) ) {
				pushDown();
			}
		}
		
		return getBest();
	}
	
	// Extracts the first event from the possibility hash
	public static Event getBest() {
		
		while( true ) {
			for( Event e: new HashSet<Event>( possibleInverse.lastEntry().getValue() ) ) {
				if( check( e ) ) return e;
				else pushDown();
			}
			// since we just recursed through all of the events on this possibility value and didn't return any, it must now be empty
			possibleInverse.remove( possibleInverse.lastKey() );
		}
	}
	
	// Checks to see if an event is actually able to achieve its possible value.
	//  If not, it stores the event in the toPush hash and returns false
	public static boolean check( Event current ) {
		if( path.get( current ) != null || possible.get( current ) == 1 ) {
			return true;
		}

		if( toPush.contains( current ) ) {
			return false;
		}
		
		for( Event e: possibleInverse.get( possible.get( current ) - 1 ) ) {
			if ( canObserveBoth( current, e ) ) {
				if( check( e ) ) {
					path.put( current, e );
					return true;
				}
				toPush.add( e );
			}
		}
		
		toPush.add( current );
		return false;
	}
	
	// Pushes all of the events down the possibility hash that were found to be in the wrong place
	public static void pushDown() {
		for( Event e: toPush ) {
			updatePossibility( e, possible.get( e ) - 1 );
		}
		toPush.clear();
	}
	
	private static void updatePossibility( Event e, int newValue ) {
		if( possible.get( e ) != null ) {
			possibleInverse.get( possible.get( e ) ).remove( e );
		}
		possibleInverse.get( newValue ).add( e );
		possible.put( e, newValue );
	}
	
	// Prunes all of the events that are not observable due to the constraint that we must observe the first and last event
	public static void pruneEvents() {
		Event start = events.get( 0 );
		Event end   = events.get( events.size() - 1 );
		ArrayList<Event> newEvents = new ArrayList<Event>();
		
		for( Event e: events ) {
			if( canObserveBoth( start, e ) && canObserveBoth( e, end ) ) {
				newEvents.add( e );
			}
		}
		events = newEvents;
	}
	
	// Determine if you can move the telescope fast enough to observe first and second (in order and given 
	//  1 unit of movement per second capability in any direction)
	public static boolean canObserveBoth( Event first, Event second ) {
		int time = second.time - first.time;
		int distance = 0;
		
		for( int i = 0; i < dim; i++ ) {
			distance += Math.abs( first.coordinates[i] - second.coordinates[i] );
		}
		return time >= distance;
	}
	
	public static void printPath( Event e ) {
		while( !check( e ) ) pushDown();

		// output the entire path until we get to the end
		Event runner = e;
		while( runner != null ) {
			System.out.println( runner );
			runner = path.get( runner );
		}
		System.out.println( "Path size: " + possible.get( e ) );
	}
	
	// Reads all of the events from filename, and sorts them if they're not already sorted
	public static void readInput( String filename ) throws FileNotFoundException {
		
		Scanner scanner = new Scanner( new File( "input/" + filename ) );
		events = new ArrayList<Event>();

		boolean sorted = true;
		int lastTime = Integer.MIN_VALUE;
		
		int amount = scanner.nextInt();
		dim = scanner.nextInt();
		
		for( int i = 0; i < amount; i++ ) {
			
			// check for sortedness while reading in our events
			int time = scanner.nextInt();
			sorted = sorted && ( time >= lastTime );
			lastTime = time;
			
			// read in our coordinates
			int[] coordinates = new int[dim];
			for( int j = 0; j < dim; j++ ) {
				coordinates[j] = scanner.nextInt();
			}
			
			events.add( new Event( time, coordinates ) );
		}
		
		scanner.close();
		if( !sorted ) sortEvents();
	}
	
	public static void sortEvents() {
		Comparator<Event> comp = new Comparator<Event>() {
			public int compare( Event e1, Event e2 ) {
				return Integer.compare( e1.time, e2.time );
			}
		};
		events.sort( comp );
	}
	
	// Writes a sample input
	public static void writeInput( String filename, int amount, int bound, int dimension ) throws Exception {
		PrintWriter writer = new PrintWriter( "input/" + filename, "UTF-8" );
		Random rand = new Random();
		
		writer.print( amount + " " + dimension );
		for( int i = 0; i < amount; i++ ) {
			writer.print( "\n" + rand.nextInt( amount ) );
			for( int dimCounter = 0; dimCounter < dimension; dimCounter++ ) {
				writer.print( " " + ( rand.nextInt( bound * 2 ) - bound ) );
			}
		}
		
		writer.close();
	}
	
}

class Event {
	public int time;
	public int[] coordinates;
	
	public Event( int time, int[] coordinate ) {
		this.time = time;
		this.coordinates = coordinate;
	}
	
	public String toString() {
		String s = Integer.toString( time ) + ":";
		for( int i = 0; i < coordinates.length; i++ ) {
			s += " " + coordinates[i];
		}
		return s;
	}
}