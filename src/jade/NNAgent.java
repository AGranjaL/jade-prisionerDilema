import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.LinkedList;

import java.util.Random;

public class NNAgent extends Agent {
    
    private State state;
    private AID mainAgent;
    private int myId, opponentId;
    private boolean playerA;
    String sState;
    private int results;
    private int[][] bestActionRow;
    private LinkedList<Double> pastActionOpponent = new LinkedList<>();
    private LinkedList<Double> pastActionMe = new LinkedList<>();
    boolean firstRound = true;
    //#id#X#N,S,R
    private int N, S, R, I, P; 
    private int result;
    //N number of players
    //S size matrix
    //R maximum number of rounds  
    private ACLMessage msg;
    private SelfOrgMaps SOM;

    protected void setup() {
        state = State.s0NoConfig;

        //Register in the yellow pages as a player
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        sd.setName("Game");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new Play());
        System.out.println("NNAgent " + getAID().getName() + " is ready.");

    }

    protected void takeDown() {
        //Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("RandomPlayer " + getAID().getName() + " terminating.");
    }

    private enum State {
        s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
    }

    private class Play extends CyclicBehaviour {
        int iAction, iActionOpponent;
        Random distValues = new Random(2000);
        int sizeGridSOM = 10;
        int sizeInputSOM = 3;

        @Override
        public void action() {
            System.out.println(getAID().getName() + ":" + state.name());
            msg = blockingReceive();
            if (msg != null) {
                //System.out.println(getAID().getName() + " received " + msg.getContent() + " from " + msg.getSender().getName()); //DELETEME
                //-------- Agent logic
                switch (state) {
                    case s0NoConfig:
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> go to state 1
                        //Else ERROR
                        if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
                            boolean parametersUpdated = false;
                            try {
                                parametersUpdated = validateSetupMessage(msg);
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                            }
                            if (parametersUpdated) state = State.s1AwaitingGame;

                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    case s1AwaitingGame:

                        //If INFORM NEWGAME#_,_ PROCESS NEWGAME --> go to state 2
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> stay at s1
                        //Else ERROR
                        //TODO I probably should check if the new game message comes from the main agent who sent the parameters
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            if (msg.getContent().startsWith("Id#")) { //Game settings updated
                                try {
                                    validateSetupMessage(msg);
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                            } else if (msg.getContent().startsWith("NewGame#")) {
                                boolean gameStarted = false;
                                firstRound = true;
                                pastActionMe = new LinkedList<>();
                                pastActionOpponent = new LinkedList<>();
                                SOM = new SelfOrgMaps(sizeGridSOM, sizeInputSOM);
                                pastActionMe.add(distValues.nextInt(S)*1.00);
                                pastActionOpponent.add(distValues.nextInt(S)*1.00);
                                pastActionMe.add(distValues.nextInt(S)*1.00);
                                pastActionOpponent.add(distValues.nextInt(S)*1.00);
                                //reset values of bestActionRow that has size of S
                                bestActionRow = new int[S][2];
                                for (int i = 0; i < S; i++ ){
                                    bestActionRow[i][0] = -1;
                                    bestActionRow[i][1] = 0;
                                }

                                try {
                                    gameStarted = validateNewGame(msg.getContent());
                                    if(myId > opponentId) playerA = false;
                                    else playerA = true; 
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                                if (gameStarted) state = State.s2Round;
                            }
                        else if (msg.getSender() != mainAgent) System.out.println("Unexpected Main Agent"+ msg.getSender());
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    case s2Round:
                        //If REQUEST POSITION --> INFORM POSITION --> go to state 3
                        //If INFORM CHANGED stay at state 2
                        //If INFORM ENDGAME go to state 1
                        //Else error
                        if (msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().startsWith("Position")) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(mainAgent);


                            if (firstRound){

                                iAction = distValues.nextInt(S);
                                firstRound = false;

                            }
                            else{
                                //get the prediction action from SOM passing the past actions as the first input and false as the second input to indicate no training
                                //covert pastActionOpponent to double array
                                double[] pastActionOpponentDouble = new double[sizeInputSOM];
                                for (int i = 0; i < pastActionOpponent.size(); i++) {
                                    pastActionOpponentDouble[i] = pastActionOpponent.get(i);
                                }
                                pastActionOpponentDouble[sizeInputSOM-1] = 0;
                                iActionOpponent = SOM.iGetBMU(pastActionOpponentDouble, false);
                                //get the best action from the bestActionRow
                                if(bestActionRow[iActionOpponent][0] != -1) iAction = bestActionRow[iActionOpponent][0];
                                else iAction = distValues.nextInt(S);

                            }
                            System.out.println("Position chosen:"+ iAction);
                            msg.setContent("Position#"+iAction);
                            System.out.println(getAID().getName() + " sent " + msg.getContent());
                            send(msg);
                            state = State.s3AwaitingResult;
                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Changed#")) {
                            // Process changed message, in this case nothing
                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("EndGame")) {
                            System.out.println(getAID().getName() + ": GAME FINISHED");
                            System.out.print("Player A:"+playerA + " RLAgent");
                            state = State.s1AwaitingGame;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message:" + msg.getContent());
                        }
                        break;
                    case s3AwaitingResult:
                        //If INFORM RESULTS --> go to state 2
                        //Else error
                        if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
                            //Process results
                            this.getResults(msg.getContent());
                            double[] pastActionOpponentDouble = new double[pastActionOpponent.size()];
                                for (int i = 0; i < pastActionOpponent.size(); i++) {
                                    pastActionOpponentDouble[i] = pastActionOpponent.get(i);
                                }
                            iActionOpponent = SOM.iGetBMU(pastActionOpponentDouble, true);
                            pastActionOpponent.removeFirst();
                            pastActionMe.removeFirst();
                            
                            state = State.s2Round;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                }
            }
        }

        /**
         * Validates and extracts the parameters from the setup message
         *
         * @param msg ACLMessage to process
         * @return true on success, false on failure
         */
        private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
            int tN, tS, tR, tI, tP, tMyId;
            String msgContent = msg.getContent();

            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3) return false;
            if (!contentSplit[0].equals("Id")) return false;
            tMyId = Integer.parseInt(contentSplit[1]);

            String[] parametersSplit = contentSplit[2].split(",");
            if (parametersSplit.length != 5) return false;
            tN = Integer.parseInt(parametersSplit[0]);
            tS = Integer.parseInt(parametersSplit[1]);
            tR = Integer.parseInt(parametersSplit[2]);
            tI = Integer.parseInt(parametersSplit[3]);
            tP = Integer.parseInt(parametersSplit[4]);

            //At this point everything should be fine, updating class variables
            mainAgent = msg.getSender();
            N = tN;
            S = tS;
            R = tR;
            I = tI;
            P = tP;
            myId = tMyId;
            return true;
        }

        /**
         * Processes the contents of the New Game message
         * @param msgContent Content of the message
         * @return true if the message is valid
         */
        public boolean validateNewGame(String msgContent) {
            int msgId0, msgId1;
            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 2) return false;
            if (!contentSplit[0].equals("NewGame")) return false;
            String[] idSplit = contentSplit[1].split(",");
            if (idSplit.length != 2) return false;
            msgId0 = Integer.parseInt(idSplit[0]);
            msgId1 = Integer.parseInt(idSplit[1]);
            if (myId == msgId0) {
                opponentId = msgId1;
                return true;
            } else if (myId == msgId1) {
                opponentId = msgId0;
                return true;
            }
            return false;
        }
        public boolean getResults(String msgContent){
            String[] contentSplit = msgContent.split("#");
            if(contentSplit.length != 3) return false;
            if(!contentSplit[0].equals("Results")) return false;
            sState = contentSplit[1]; //state = x,y
            int stateA = Integer.parseInt(sState.charAt(0)+"");
            int stateB = Integer.parseInt(sState.charAt(2)+"");
            String[] resultsSplit = contentSplit[2].split(",");
            if(resultsSplit.length != 2) return false;
            if(playerA) {
                //if its player a then result is before the coma
                result = Integer.parseInt(resultsSplit[0]);
                results += result;
                pastActionMe.add(stateA*1.00); //this hasnt been used yet, might add it to the SOM input
                pastActionOpponent.add(stateB*1.00); //actions of the opponent to predict with SOM
                if(bestActionRow[stateB][0] == -1 || (bestActionRow[stateB][0] != -1 && bestActionRow[stateB][1] < result) ) {
                    bestActionRow[stateB][0] = stateA;
                    bestActionRow[stateB][1] = result;
                }
                //result = Integer.parseInt(resultsSplit[0]) - Integer.parseInt(resultsSplit[1]);
            }
            else{
                result = Integer.parseInt(resultsSplit[1]);
                results += result;
                pastActionMe.add(stateA*1.00);
                pastActionOpponent.add(stateB*1.00);
                if(bestActionRow[stateA][0] == -1 || (bestActionRow[stateA][0] != -1 && bestActionRow[stateA][1] < result) ) {
                    bestActionRow[stateA][0] = stateB;
                    bestActionRow[stateA][1] = result;
                }
                //result = Integer.parseInt(resultsSplit[1]) - Integer.parseInt(resultsSplit[0]);
            }

            return true;

        }
    }
}
