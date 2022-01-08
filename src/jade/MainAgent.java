import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;
import java.util.Vector;

import java.io.PrintStream;
import java.util.ArrayList;

public class MainAgent extends Agent {

    private GUI gui;
    private AID[] playerAgents;
    public GameParametersStruct parameters = new GameParametersStruct();
    private Vector<Integer>[][] payoffMatrix; 
    private Object[] workAround;
    private String[][] playerLogs;
    private GameInformation gameInfo;

    @Override
    protected void setup() {
        gui = new GUI(this);
        System.setOut(new PrintStream(gui.getLoggingOutputStream()));
        updatePlayers();
        gui.logLine("Agent " + getAID().getName() + " is ready.");
    }

    public int updatePlayers() {
        gui.logLine("Updating player list");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                gui.logLine("Found " + result.length + " players");
            }
            playerAgents = new AID[result.length];
            for (int i = 0; i < result.length; ++i) {
                playerAgents[i] = result[i].getName();
            }
        } catch (FIPAException fe) {
            gui.logLine(fe.getMessage());
        }
        //Provisional
        String[] playerNames = new String[playerAgents.length];
        for (int i = 0; i < playerAgents.length; i++) {
            playerNames[i] = playerAgents[i].getName().split("@")[0];


        }
        gui.setPlayersUI(playerNames);
        return 0;                
    }
    public String getPlayerInfo(String playerAgentName) {
        String splayerLogs = "";
        for (int i = 0; i < playerAgents.length; i++) {
            if (playerAgents[i].getName().split("@")[0].equals(playerAgentName.split(" - ")[1])) {
                //return all the info about the player

                for (int j = 0; j < gameInfo.nGames; j++) {
                    splayerLogs += playerLogs[i][j];
                    }
                return "Name:" + playerAgents[i].getName().split("@")[0] + ": " + splayerLogs;
                }

            }
        return "";
        }



    public void setParameter(String parameter, int value){
        //with switch clause set parameter from gameparameterstruct with the value
        switch(parameter){
            case "R":                
                parameters.R = value;
                break;
            case "N":
                parameters.N = value;
                break;
            case "S":
                parameters.S = value;
                break;
            case "I":
                parameters.I = value;
                break;
            case "P":
                parameters.P = value;
                break;

        }


    }
    public Object[][] updateRoundPoints(){
        Object[] [] roundPoints = new Object[playerAgents.length][gameInfo.nGames+1];
        for (int i = 0; i < playerAgents.length; i++){
            roundPoints[gameInfo.players.get(i).id][0] = playerAgents[i].getName().split("@")[0];
        }
        for(int i = 0; i < playerAgents.length; i++){
            for(int j = 1; j < gameInfo.nGames+1; j++){
                //System.out.println(gameInfo.results[i][j-1]);
                roundPoints[i][j] = gameInfo.results[i][j-1];
            }
        }
        return roundPoints;
    }
    public Object[] getGameNames(){
        Integer[] workAround = new Integer[gameInfo.nGames+1];
        for(int i = 0; i < gameInfo.nGames+1 ; i++){
            workAround[i] = i;
        }
        return workAround;
    }

    public int newGame() {
        this.payoffMatrix = payoffMatrix();
        this.workAround = workAround();
        addBehaviour(new GameManager());
        return 0;
    }
    public Vector<Integer>[][] payoffMatrix(){
        int upperbound = 10;
        @SuppressWarnings("unchecked")
        Vector<Integer>[][] payoffMatrix = new Vector[parameters.S][parameters.S];

        Random pos1 = new Random();
        Random pos2 = new Random();
        Vector<Integer> vec = new Vector<Integer>(2);
        for(int i = 0; i<parameters.S; i++){
            for(int j = 0; j<parameters.S; j++){
                if (i == j){
                    payoffMatrix[i][j] = new Vector<Integer>(2);
                    payoffMatrix[i][j].add(pos1.nextInt(upperbound));
                    payoffMatrix[i][j].add(pos2.nextInt(upperbound));

                }
                else if(j>i){
                    vec.add(pos1.nextInt(upperbound));
                    vec.add(pos2.nextInt(upperbound));
                    payoffMatrix[i][j] = new Vector<Integer>(2);
                    payoffMatrix[i][j].add(vec.elementAt(0));
                    payoffMatrix[i][j].add(vec.elementAt(1));
                    payoffMatrix[j][i] = new Vector<Integer>(2);
                    payoffMatrix[j][i].add(vec.elementAt(1));
                    payoffMatrix[j][i].add(vec.elementAt(0));
                    vec.clear();
                }

            }
     

        }

        return payoffMatrix;
        
    }
    public Object[] workAround(){
        Integer[] workAround = new Integer[parameters.S];
        for(int i = 0; i < parameters.S ; i++){
            workAround[i] = i;
        }
        return workAround;

    } 
    public Vector<Integer>[][] getPayoffMatrix(){
        return this.payoffMatrix;
    }
    public Object[] getWorkAround(){
        return this.workAround;
    }

    /**
     * In this behavior this agent manages the course of a match during all the
     * rounds.
     */
    private class GameManager extends SimpleBehaviour {

        @Override
        public void action() {
            //Assign the IDs
            ArrayList<PlayerInformation> players = new ArrayList<>();
            int lastId = 0;
            for (AID a : playerAgents) {
                players.add(new PlayerInformation(a.getName().split("@")[0],a, lastId++));
            }
            parameters.N = players.size();
            int nGames = parameters.N * (parameters.N - 1) / 2;
            gameInfo = new GameInformation(nGames, players);
            playerLogs = new String[players.size()][nGames];
            for (int i = 0; i < players.size(); i++) {
                for (int j = 0; j < nGames; j++) {
                    playerLogs[i][j] = "";
                }
            }


            //Initialize (inform ID)
            for (PlayerInformation player : players) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Id#" + player.id + "#" + parameters.N + "," + parameters.S + "," + parameters.R + "," + parameters.I + "," + parameters.P);
                msg.addReceiver(player.aid);
                send(msg);
            }
            //Organize the matches
            int iGame = 0;
            for (int i = 0; i < players.size()-1; i++) {
                for (int j = i + 1; j < players.size(); j++) { 
                    iGame++;
                    playGame(players.get(i), players.get(j), iGame);
                    updateRoundPoints();
                    gui.updatePanel();
                    
                }
            }
            for(int i = 0; i < players.size(); i++){
                System.out.println(players.get(i).aid + " " + players.get(i).score);

            }


            //arrange playerNames by score in descending order
            for (int i = 0; i < players.size() - 1; i++) {
                for (int j = i + 1; j < players.size(); j++) {
                    if (players.get(i).score < players.get(j).score) {
                        PlayerInformation aux = players.get(i);
                        players.set(i, players.get(j));
                        players.set(j, aux);
                    }
                }
            }
            String[] playerNames = new String[players.size()];
            for (int i = 0; i < players.size(); i++) {
                String name = players.get(i).aid.getName().split("@")[0];
                playerNames[i] = (i+1) + "º - "+ name + " - Score:" + players.get(i).score;
            }
            gui.setPlayersUI(playerNames);



           
        }
 

        private void playGame(PlayerInformation player1, PlayerInformation player2, int iGame) {
            //Assuming player1.id < player2.id
            int played_rounds = 0;
            int results[]= {0,0};
            System.out.println("NEW GAME"+player1.aid.getName()+" - "+player2.aid.getName());
           
                
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(player1.aid);
            msg.addReceiver(player2.aid);
            msg.setContent("NewGame#" + player1.id + "," + player2.id);
            send(msg);

            int pos1, pos2;
            while(played_rounds < parameters.R){
                System.out.println("ROUND "+ (played_rounds+1));
                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("Position");
                msg.addReceiver(player1.aid);
                send(msg);

                //gui.logLine("Main Waiting for movement");
                ACLMessage move1 = blockingReceive();
                //gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
                pos1 = Integer.parseInt(move1.getContent().split("#")[1]);

                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("Position");
                msg.addReceiver(player2.aid);
                send(msg);

                //gui.logLine("Main Waiting for movement");
                ACLMessage move2 = blockingReceive();
                //gui.logLine("Main Received " + move2.getContent() + " from " + move2.getSender().getName());
                pos2 = Integer.parseInt(move2.getContent().split("#")[1]);

                msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(player1.aid);
                msg.addReceiver(player2.aid);
                int res1, res2;
                res1 = payoffMatrix[pos1][pos2].elementAt(0);
                res2 = payoffMatrix[pos1][pos2].elementAt(1);
                results[0] += res1;
                results[1] += res2;
                msg.setContent("Results#" + pos1 + "," + pos2 + "#" + res1 + "," + res2);
                send(msg);
                played_rounds++;
            }
            msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(player1.aid);
            msg.addReceiver(player2.aid);
            msg.setContent("EndGame");
            send(msg);
            if(results[0] > results[1]) player1.score+= 3;
            else if (results[0] == results[1]) {
                player1.score += 1;
                player2.score += 1;
            }
            else player2.score += 3;
            //add the results to the GUI with setPlayers method in descending order according to score
            gameInfo.setResults(player1, player2, results[0], results[1], iGame);
            playerLogs[player1.id][iGame-1] =  "\nGame"+ iGame + ":" + player1.score;
            playerLogs[player2.id][iGame-1] =  "\nGame"+ iGame + ":" + player2.score;

            System.out.println("RESULTS: -PlayerA:"+results[0]+"  -PlayerB:"+ results[1]);





        }

        @Override
        public boolean done() {
            return true;
        }
    }
    public class GameInformation {
        int nGames;
        ArrayList<PlayerInformation> players;
        int[][] results; // [playeri][Gamei]
        public GameInformation(int nGames, ArrayList<PlayerInformation> players) {
            this.nGames = nGames;
            this.players = players;
            results = new int[players.size()][nGames];
            for (int i = 0; i < nGames; i++) {
                for (int j = 0; j < players.size(); j++) {
                    results[j][i] = 0;
                }
            }

        }
        public void setResults(PlayerInformation p1, PlayerInformation p2 , int res1, int res2, int iGame){
            results[p1.id][iGame-1] = res1;
            results[p2.id][iGame-1] = res2;

        }

        }

        
    
    public class PlayerInformation {
        String name;
        AID aid;
        int id;
        int score;

        public PlayerInformation(String name, AID a, int i) {
            aid = a;
            id = i;
            score = 0;
        }

        @Override
        public boolean equals(Object o) {
            return aid.equals(o);
        }
    }

    public class GameParametersStruct {

        int N;
        int S;
        int R;
        int I;
        int P;

        public GameParametersStruct() {
            N = 2;
            S = 4;
            R = 200;
            I = 0;
            P = 10;
        }
    }
}
