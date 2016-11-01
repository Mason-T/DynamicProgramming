package telescope;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

public class Main {

	static int dim;
	static ArrayList<Event> events;
	static HashMap<Event, Event> path;  // if there is a known path to the end from an event, the next event to be observed is stored here
	static HashMap<Event, Integer> possible;  // records the current maximum number of possible events that each event can observe after itself (even if the path is not known)
	static TreeMap<Integer, HashSet<Event>> possibleInverse;  // a reverse hash of possible

	public static void main( String[] args ) throws Exception {
		writeInput( "inputrand", 100000, 100, 3 );
		readInput( "inputrand" );
		pruneEvents();
		
		long time = System.currentTimeMillis();
		Event e = findMaxObservable();
		printPath( e );
		System.out.println( "Time taken: " + ( System.currentTimeMillis() - time ) );
	}
	
	public static Event findMaxObservable() {
		path = new HashMap<Event, Event>();
		possible = new HashMap<Event, Integer>();
		possibleInverse = new TreeMap<Integer, HashSet<Event>>();
		possibleInverse.put( 1, new HashSet<Event>() );
		
		findPaths();
		
		
		if( possibleInverse.lastEntry().getValue().isEmpty() ) {
			possibleInverse.remove( possibleInverse.lastKey() );
		}
		
		Event best = null;
		for( Event e: new HashSet<Event>( possibleInverse.lastEntry().getValue() ) ) {
			if( check( e ) ) best = e;
			else pushDown( e );
		}
		
		return best;
	}
	
	public static void findPaths() {
		
		for( int i = events.size() - 1; i >= 0; i-- ) {
			Event event = events.get( i );
			int currentMax = possibleInverse.lastKey();
			if( !possibleInverse.get( currentMax ).isEmpty() ) {
				possibleInverse.put( ++currentMax, new HashSet<Event>() );
			}
			possibleInverse.get( currentMax ).add( event );
			possible.put( event, currentMax );
			if( !check( event ) ) {
				pushDown( event );
			}
		}
	}
	
	// Checks to see if an event is actually able to achieve its possible value.
	//  If not, it pushes the event further down the possible stack and returns false
	public static boolean check( Event current ) {
		if( path.get( current ) != null ) {
			return true;
		}
		
		if( possible.get( current ) == 1 ) {
			path.put( current, current );
			return true;
		}
		
		int currentPossible = possible.get( current );
		HashSet<Event> toPush = new HashSet<Event>();
		
		for( Event e: possibleInverse.get( currentPossible - 1 ) ) {
			if ( canObserveBoth( current, e ) ) {
				if( check( e ) ) {
					path.put( current, e );
					pushDown( toPush );
					return true;
				}
				toPush.add( e );
			}
		}
		
		pushDown( toPush );
		return false;
	}
	
	public static void pushDown( Collection<Event> events ) {
		for( Event e: events ) {
			pushDown( e );
		}
	}
	
	public static void pushDown( Event e ) {
		possibleInverse.get( possible.get( e ) ).remove( e );
		possible.put( e, possible.get( e ) - 1 );
		possibleInverse.get( possible.get( e ) ).add( e );
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
		return ( time >= distance );
	}
	
	public static void printPath( Event e ) {
		while( !check( e ) ) pushDown( e );
		
		System.out.println( e );
		Event runner = e;
		
		// output the entire path until we get to the end
		while( runner != path.get( runner ) ) {
			runner = path.get( runner );
			System.out.println( runner );
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
			sorted = sorted && time >= lastTime;
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