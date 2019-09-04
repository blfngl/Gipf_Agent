package csc3335.Agent;

//import csc3335.Agent.GetNewState;

import csc3335.Agent.FinalAgent;

import java.util.*;

import csc3335.gipf_game.GipfGame;

class MCTS {

    public Node root;
    public ArrayList<Node> nextMoves = new ArrayList<Node>();
    FinalAgent agent;
    private Random rand;
    String previousMove;

    /**
     * Node class:
     * Game state
     * State value
     * Player turn
     * Move taken to get to node state
     */
    class Node implements Comparable {
        GipfGame game;
        public int playerTurn;
        public int value;
        public String moveTaken;

        public int getValue() {
            return value;
        }

        public Node(GipfGame g, int playerTurn) {
            this.game = g;
            this.playerTurn = playerTurn;
        }

        @Override
        public int compareTo(Object o) {
            Node n = (Node) o;
            return value < n.getValue() ? n.value : value;
        }
    }


    /**
     * Constructor
     */
    public MCTS(GipfGame game, int playerTurn, FinalAgent a) {

        FinalAgent agent = a;
        this.agent = agent;
        GipfGame g = new GipfGame(game);

        this.root = new Node(g, playerTurn);
    }


    /**
     * Search:
     * Takes in the current game state and the players turn
     * it then calls init function which builds the first layer of the tree
     * it then calls traverse and update
     */
    public String search(GipfGame game, int playerTurn) {
        GipfGame g = new GipfGame(game);
        this.root = new Node(g, playerTurn);

        this.init();


        for (Node n : nextMoves) {
            int newV = traverseAndUpdate(n, 0, root.playerTurn);
            n.value += (int) .5 * newV;
            n.value = Math.abs(n.value);
        }


        Node bestMove = nextMoves.get(0);
        Node nextBest = nextMoves.get(1);

        Arrays.sort(new ArrayList[]{nextMoves});

        for (Node n : nextMoves) {
//            System.out.print(n.value+"  ");
            if (Math.abs(n.value) > Math.abs(bestMove.value)) {
                bestMove = n;
            }
            if (Math.abs(n.value) > Math.abs(nextBest.value) && Math.abs(n.value) < Math.abs(bestMove.value)) {
                nextBest = n;
            }
        }


//        System.out.println(bestMove.value);

        if (bestMove.moveTaken == previousMove) {
            return nextBest.moveTaken;
        }

        return bestMove.moveTaken;
    }


    /**
     * Builds first layer of the tree
     */
    public void init() {

        for (Map.Entry<Integer, String> move : FinalAgent.moves.entrySet()) {

            GipfGame newGame = new GipfGame(root.game);

            Node n = new Node(newGame, root.playerTurn);

            if (n.game.makeMove(move.getValue(), n.playerTurn)) {
                int v = agent.evaluate(n.game, move.getValue());
                n.value = v;
                n.moveTaken = move.getValue();
                this.nextMoves.add(n);
            }
        }
    }

    /**
     * Checks if the game is finished
     */
    private int gameOver(GipfGame g) {

        if (g.getPiecesLeft(0) == 0) {
            return 2;
        }
        if (g.getPiecesLeft(1) == 0) {
            return 1;
        }
        if (g.getPiecesLeft(0) == 0) {
            return 2;
        }
        if (g.getPiecesLeft(1) == 0) {
            return 1;
        }
        return 0;
    }


    /**
     * Traverse and update takes in a node a depth and the a player
     * it recursively simulates the random moves in down the tree
     * switching players every call.
     * It returns the evaluation of each recursive node and adds it
     * to the root node which is one of the nodes in the first layer of the tree
     */
    public int traverseAndUpdate(Node n, int depth, int player) {
        int power = 0;
        boolean result = false;
        Node m = new Node(new GipfGame(n.game), player);

        if (depth == 20 || gameOver(n.game) != 0) {

            return agent.evaluate(m.game, agent.getRandomMove());
        } else {
            do {
                result = m.game.makeMove(agent.getRandomMove(), player);
            }
            while (!result);

            return power + traverseAndUpdate(m, ++depth, player == 0 ? 1 : 0);
        }
    }
}
