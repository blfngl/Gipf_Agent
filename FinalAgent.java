
package csc3335.Agent;

import csc3335.gipf_game.GipfGame;
import csc3335.gipf_game.GipfPlayable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

public class FinalAgent implements GipfPlayable
{
	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int CENTER = 2;
	private static final String ESCAPE = "escape_string";

	Scanner scan = new Scanner(System.in);
	
	// The actual game instance to make moves on
	private GipfGame theGame;
	MCTS newTree;
	ArrayList<String> bestMoves = new ArrayList<>();

	// Keeps track of the search runtime
	private long startingTime;
	private boolean hasSetPlayers = false;
	private int playerMe, playerThem, bestMovePower;
	private String bestMove;
	private Random rand;

	// Agent's perception of the boardstate
	private Integer[][] boardState;
	private String[] edgeSpots;

	private final String[] boardSpots = {
			"b 2", "b 3", "b 4", "b 5",
			"c 2", "c 3", "c 4", "c 5", "c 6",
			"d 2", "d 3", "d 4", "d 5", "d 6", "d 7",
			"e 2", "e 3", "e 4", "e 5", "e 6", "e 7", "e 8",
			"f 2", "f 3", "f 4", "f 5", "f 6", "f 7",
			"g 2", "g 3", "g 4", "g 5", "g 6",
			"h 2", "h 3", "h 4", "h 5"
	};

	private final char[] letterValues = "abcdefghi".toCharArray();
	public static HashMap<Integer, String> moves;
	private HashMap<String, Integer> boardPositions;

	public FinalAgent(GipfGame game)
	{
		this.theGame = game;
		initAgent();
		rand = new Random();

		edgeSpots = game.getEdgeSpots();
		bestMovePower = 0;
		// Default best move
		bestMove = "a 1 1";

		this.newTree = new MCTS(game, playerMe,this);
		boardState = this.theGame.getBoardCopy();
	}

	/**
	 * This method returns the power level of a move.
	 * @param game The game to check.
	 * @param move The move to evaluate.
	 * @return The power level of the move.
	 */
	public int evaluate(GipfGame game, String move)
	{
		int movePower = 0;

		movePower +=  2* game.getPiecesLeft(playerMe);
		movePower -=  game.getPiecesLeft(playerThem);
		// Evaluate default direction (1)
		// First check for enemy runs
		int theirRunSize = getRunSize(game, move, playerThem);

		// Then check for our runs
		int ourRunSize = getRunSize(game, move, playerMe);

		if (ourRunSize <= 0)
			movePower += 5;

		// Make sure an enemy run of 3 is stopped
		 if (theirRunSize == 3)
			movePower += 30;

		// Complete our runs if we have any
		if (ourRunSize == 3)
			movePower += 20;

		// Then check for our 2s
		 if (ourRunSize == 2)
			movePower += 2;

		// Then check for enemy 2s
		 if (theirRunSize == 2)
			movePower += 5;

		// Then find a piece of ours
		 if (ourRunSize == 1)
			movePower += 15;

		 if (theirRunSize == 1)
			movePower -= 5;

		return Math.abs(movePower);
	}


	/**
	 * This method returns the size of the run made for a specific player
	 * by a given move.
	 * @param move The move to evaluate
	 * @param player The player making the move
	 * @return The size of the run created.
	 */
	private int getRunSize(GipfGame game, String move, int player)
	{
		boolean shouldContinue = true;
		int runSize = 0;
		// Get the starting edge from the move
		String startingEdge = move.substring(0, 3);
		int direction = Integer.parseInt(move.substring(4, 5));
		// A run always starts one in from the edge
		String nextSpot = getNextSpot(startingEdge, startingEdge, direction);

		while (!nextSpot.equals(ESCAPE) && shouldContinue)
		{
			String currentSpot = nextSpot;
			int piece = game.getBoardState(currentSpot);

			// Run size goes up and operation continues if the piece belongs
			// to the given player
			if (piece == player + 1 || piece == player + 3)
			{
				runSize++;
				// Now check the next spot
				nextSpot = getNextSpot(startingEdge, currentSpot, direction);
			}

			// This breaks the run if the piece belongs to the opposite player
			else
				shouldContinue = false;
		}

		return runSize;
	}

	/**
	 * This method returns the next spot from a given direction. It functions primarily
	 * as a helper method for getRunSize().
	 * 
	 * This method is fairly complicated and I hope to be able to simplify it,
	 * but I haven't put in the effort to yet.
	 * 
	 * @param startingSpot The spot the run started in.
	 * @param spotToCheck The spot to get the next spot from.
	 * @param direction The direction to travel.
	 * @return The next spot, if valid. Escape string is returned if invalid.
	 */
	public String getNextSpot(String startingSpot, String spotToCheck, int direction)
	{
		String nextSpot = "";
		int startSpotSide = getSpotSide(startingSpot);

		String currentSpotColumn = spotToCheck.substring(0, 1);
		int currentSpotPosition = Integer.parseInt(spotToCheck.substring(2, 3));
		int currentSpotSide = getSpotSide(spotToCheck);

		/*System.out.println("Starting side: " + startSpotSide +
				"\nCurrent side: " + currentSpotSide +
				"\nCurrent spot pos: " + currentSpotPosition +
				"\nDirection: " + direction);*/

		// Up and down are super easy to handle, as this applies to all spots
		// regardless of starting position
		if (direction == 0)
			nextSpot += currentSpotColumn + " " + (currentSpotPosition + 1);

		else if (direction == 3)
			nextSpot += currentSpotColumn + " " + (currentSpotPosition - 1);

		// Otherwise, if the start is on the left
		else if (startSpotSide == LEFT)
		{
			String nextColumn = "" + letterValues[getColumnIndex(currentSpotColumn) + 1];

			if (direction == 1)
			{
				if (currentSpotSide == CENTER || currentSpotSide == RIGHT)
					nextSpot += nextColumn + " " + currentSpotPosition;

				else if (currentSpotSide == LEFT)
					nextSpot += nextColumn + " " + (currentSpotPosition + 1);
			}

			else if (direction == 2)
			{
				if (currentSpotSide == CENTER || currentSpotSide == RIGHT)
					nextSpot += nextColumn + " " + (currentSpotPosition - 1);

				else if (currentSpotSide == LEFT)
					nextSpot += nextColumn + " " + currentSpotPosition;
			}
		}

		// If start is on the right
		else if (startSpotSide == RIGHT)
		{
			String nextColumn = "" + letterValues[getColumnIndex(currentSpotColumn) - 1];

			if (direction == 4)
			{
				if (currentSpotSide == CENTER || currentSpotSide == LEFT)
					nextSpot += nextColumn + " " + (currentSpotPosition - 1);

				else if (currentSpotSide == RIGHT)
					nextSpot += nextColumn + " " + currentSpotPosition;
			}

			else if (direction == 5)
			{
				if (currentSpotSide == CENTER || currentSpotSide == LEFT)
					nextSpot += nextColumn + " " + currentSpotPosition;

				else if (currentSpotSide == RIGHT)
					nextSpot += nextColumn + " " + (currentSpotPosition + 1);
			}
		}

		//System.out.println("Next spot: " + nextSpot);

		// Confirm the spot is valid (not an edge)
		if (isValidBoardSpot(nextSpot))
			return nextSpot;

		// If the spot doesn't exist or is an edge
		return ESCAPE;
	}

	/**
	 * Returns true if a string is a valid board spot (excludes edges).
	 * @param string The string to check.
	 * @return True if the string is a valid spot, false otherwise.
	 */
	private boolean isValidBoardSpot(String string)
	{
		for (String spot: boardSpots)
			if (spot.equals(string))
				return true;

		return false;
	}

	/**
	 * This method sets the directions the getRunSize method should move
	 * while looking for pieces.
	 * 
	 * The valid directions should be 0-5.
	 * @param direction The direction to convert.
	 */


	/**
	 * Returns true if the spot provided is an edge.
	 * @param spot The spot to check.
	 * @return true if the spot is an edge, false otherwise.
	 */


	/**
	 * This method updates the agent's perception of a given game.
	 * @param game The game to observe.
	 */
	private void updateState(GipfGame game)
	{
		boardState = game.getBoardCopy();
	}

	/**
	 * This method returns the side a spot is. Necessary for finding
	 * the next spot when checking runs.
	 * @param spot The edge to check
	 * @return Left (a, b, c, d), right (f, g, h, i) or center (e)
	 */
	private int getSpotSide(String spot)
	{
		String col = spot.substring(0, 1);

		if (col.equals("a") || col.equals("b") || col.equals("c") || col.equals("d"))
			return LEFT;

		else if (col.equals("e"))
			return CENTER;

		return RIGHT;
	}

	/**
	 * Returns the index of a given column.
	 * @param column The column to retrieve the index of.
	 * @return The index of the given column.
	 */
	private int getColumnIndex(String column)
	{
		switch(column)
		{
		case "a": return 0;
		case "b": return 1;
		case "c": return 2;
		case "d": return 3;
		case "e": return 4;
		case "f": return 5;
		case "g": return 6;
		case "h": return 7;
		case "i": return 8;
		}

		return 0;
	}

	/**
	 * This method returns a random edge spot.
	 * @return An edge spot.
	 */
	public String getRandomMove()
	{
		return moves.get(rand.nextInt(moves.size()));
	}

	private void initAgent()
	{

		// Add all possible moves to the agent
		int key = 0;
		moves = new HashMap<Integer, String>();

		moves.put(key++, "a 1 1");
		moves.put(key++, "a 2 1");
		moves.put(key++, "a 2 2");
		moves.put(key++, "a 3 1");
		moves.put(key++, "a 3 2");
		moves.put(key++, "a 4 1");
		moves.put(key++, "a 4 2");
		moves.put(key++, "a 5 2");

		moves.put(key++, "b 1 0");
		moves.put(key++, "b 1 1");
		moves.put(key++, "b 6 2");
		moves.put(key++, "b 6 3");

		moves.put(key++, "c 1 0");
		moves.put(key++, "c 1 1");
		moves.put(key++, "c 7 2");
		moves.put(key++, "c 7 3");

		moves.put(key++, "d 1 0");
		moves.put(key++, "d 1 1");
		moves.put(key++, "d 8 2");
		moves.put(key++, "d 8 3");

		moves.put(key++, "e 1 0");
		moves.put(key++, "e 9 3");

		moves.put(key++, "f 1 0");
		moves.put(key++, "f 1 5");
		moves.put(key++, "f 8 3");
		moves.put(key++, "f 8 4");

		moves.put(key++, "g 1 0");
		moves.put(key++, "g 1 5");
		moves.put(key++, "g 7 3");
		moves.put(key++, "g 7 4");

		moves.put(key++, "h 1 0");
		moves.put(key++, "h 1 5");
		moves.put(key++, "h 6 3");
		moves.put(key++, "h 6 4");

		moves.put(key++, "i 1 5");
		moves.put(key++, "i 2 4");
		moves.put(key++, "i 2 5");
		moves.put(key++, "i 3 4");
		moves.put(key++, "i 3 5");
		moves.put(key++, "i 4 4");
		moves.put(key++, "i 4 5");
		moves.put(key++, "i 5 4");
	}

	@Override
	public String makeGipfMove(int curPlayer)
	{
		updateState(theGame);


		if (!hasSetPlayers) {
			playerMe = curPlayer;
			playerThem = playerMe == 0 ? 1 : 0;
			hasSetPlayers = true;
		}

		return  newTree.search(theGame, playerMe);
	}
}
